/*
 * Copyright (c) 2016, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The RACE - Runtime for Airspace Concept Evaluation platform is licensed
 * under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gov.nasa.race.core

import java.io.FileInputStream

import akka.actor._
import akka.event.Logging.LogLevel
import akka.event.LoggingAdapter
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import gov.nasa.race._
import gov.nasa.race.common.SettableClock
import gov.nasa.race.common.Status._
import gov.nasa.race.config.ConfigUtils._
import gov.nasa.race.core.Messages._
import gov.nasa.race.util.FileUtils._
import gov.nasa.race.util.NetUtils._
import gov.nasa.race.util.{ClassLoaderUtils, DateTimeUtils}
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.collection._
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps


object RaceActorSystem { // aka RAS

  private val liveSystems = TrieMap[ActorSystem, RaceActorSystem]()
  // all RaceActors live in RaceActorSystems, no need to use Option
  def apply(system: ActorSystem): RaceActorSystem = liveSystems.get(system).get

  var terminationListeners = Set.empty[() => Unit]
  def addTerminationListener(listener: () => Unit) = terminationListeners = terminationListeners + listener
  def addTerminationAction(action: => Unit) = terminationListeners = terminationListeners + (() => action)
  def removeTerminationListener(listener: () => Unit) = terminationListeners = terminationListeners - listener

  def hasLiveSystems = liveSystems.nonEmpty
  def numberOfLiveSystems = liveSystems.size
  def addLiveSystem(race: RaceActorSystem) = liveSystems += (race.system -> race)
  def removeLiveSystem(race: RaceActorSystem) = {
    liveSystems -= race.system
    if (liveSystems.isEmpty) {
      // we need to do this sync since the logging might not show anymore
      println("last actor system did shut down, exiting RACE\n")
      terminationListeners.foreach(_())

      if (!isRunningEmbedded) System.exit(0)
    }
  }

  def shutdownLiveSystems = {
    liveSystems.values.foreach(_.terminate)
  }

  private var isRunningEmbedded = false
  def runEmbedded = isRunningEmbedded = true
}

/**
 * RaceActorSystem (RAS) instances represent a Akka actor system comprised of RaceActors, managed
 * by a single (non-RaceActor) Master, communicating through a Bus that allows for local and
 * remote publish/subscribe.
 *
 * RaceActorSystems are instantiated by the respective Main, providing a Config object
 * specifying its RaceActors.
 *
 * At runtime, a RAS is mostly used to aggregate information that is needed by its
 * RaceActors, i.e. it doesn't play an active role except of controlling termination
 * policies.
 *
 * NOTE - the RaceActorSystem instance is shared between all local actors. Make sure
 * this doesn't create race conditions (exposed data should be invariant after init,
 * or at least thread safe)
 *
 * We can't easily turn this into a Akka extension since we have to be in control of
 * when&where RAS instances are created. The RAS owns the Akka 'system', not the other way
 */
class RaceActorSystem(val config: Config) extends LogController with VerifiableAsker {
  import gov.nasa.race.core.RaceActorSystem._

  protected var status = Initializing
  val name = config.getString("name")
  val system = createActorSystem(name, config)
  implicit val log = getLoggingAdapter(system)
  val classLoader = ClassLoaderUtils.setRaceClassloader(system, config.getOptionalString("classpath"))
  val bus = createBus(system)

  //--- clock initialization and delays
  val simClock = createSimClock
  var wallStartTime = wallClockStartTime(false) // will be re-evaluated during launch
  val delayLaunch = config.getBooleanOrElse("delay-launch", false)

  //--- specific timeouts
  val defaultSystemTimeout: FiniteDuration = config.getFiniteDurationOrElse("system-timeout",timeout.duration*2)
  val defaultActorTimeout: FiniteDuration = config.getFiniteDurationOrElse("actor-timeout",timeout.duration)
  val createTimeout = Timeout(config.getFiniteDurationOrElse("create-timeout", defaultSystemTimeout))
  val initTimeout = Timeout(config.getFiniteDurationOrElse("init-timeout", defaultSystemTimeout))
  val startTimeout = Timeout(config.getFiniteDurationOrElse("start-timeout", defaultSystemTimeout))

  // do we allow external (remote) termination
  val allowRemoteTermination = config.getBooleanOrElse("remote-termination", false)
  // do we allow our own actors to trigger termination
  val allowSelfTermination = config.getBooleanOrElse("self-termination", false)

  loadSystemProperties

  addLiveSystem(this)
  system.whenTerminated.foreach( _ => removeLiveSystem(this))

  RaceLogger.logController = this
  debug(s"initializing RaceActorSystem for config:\n${showConfig(config)})")

  // updated by RaceActor ctor during creation - use only typesafe values here
  // TODO - consolidate

  var actorCapabilities = Map.empty[ActorRef,RaceActorCapabilities]
  var commonCapabilities = RaceActorCapabilities.AllCapabilities  // to quickly check if operations such as clock reset are permitted

  var actors = ListMap.empty[ActorRef, Config]

  // remote RAS book keeping
  var usedRemoteMasters = Map.empty[UrlString, ActorRef] // the remote masters we use (via our remote actors)
  var usingRemoteMasters = Set.empty[ActorRef] // the remote masters that use us (some of our actors are their remotes)

  val master = system.actorOf(Props(getMasterClass, this), name)
  waitForActor(master) {
    case e =>
      error(s"error instantiating master of $name: $e")
      throw new RaceInitializeException("no master for $name")
  }

  // this needs to be here so that all local actors can get it (before they get created!)
  val localRaceContext = createRaceContext(master, bus)

  createActors
  if (status == Created) initializeActors

  if (status != Initialized) {
    system.terminate()
    throw new RaceInitializeException("race actor system did not initialize")
  }

  // done with initialization

  // called by actor ctors during instantiation
  def registerActor (actorRef: ActorRef, actorConf: Config, actorCaps: RaceActorCapabilities) = {
    info(s"register actor ${actorRef.path.name}")
    actors = actors + (actorRef -> actorConf)
    actorCapabilities = actorCapabilities + (actorRef -> actorCaps)
    commonCapabilities = commonCapabilities.intersection(actorCaps)
  }

  //--- those can be overridden by subclasses

  def loadSystemProperties: Unit = {
    // load from file
    ifSome(config.getOptionalFile("properties")){ propFile =>
      using(new FileInputStream(propFile)) { fis => System.getProperties.load(fis) }
    }

    // now check if we have explicitly set properties
    ifSome(config.getOptionalConfig("property")) { pconf =>
      pconf.entrySet.asScala.foreach{ e=> System.setProperty(e.getKey, e.getValue.unwrapped.toString) }
    }
  }

  /**
    * either schedule the start if there is a configured start time, or start right away if not
    *
    * @return true if status is started
    */
  def launch: Boolean = {
    wallStartTime = wallClockStartTime(true)
    wallStartTime match {
      case Some(startDate) => scheduleStart(startDate)
      case None => startActors
    }
    status == Started
  }

  def createActorSystem(name: String, conf: Config): ActorSystem = ActorSystem(name, config)

  def getLoggingAdapter(sys: ActorSystem): LoggingAdapter = sys.log

  def createBus(sys: ActorSystem): Bus = new Bus(sys)

  def getMasterClass: Class[_ <: Actor] = classOf[MasterActor]

  def createSimClock: SettableClock = {
    val startTime = config.getDateTimeOrElse("start-time", DateTime.now) // in sim time
    val timeScale = config.getDoubleOrElse("time-scale", 1.0)
    val endTime = config.getOptionalDateTime("end-time") orElse { // both in sim time
      config.getOptionalFiniteDuration("run-for") map (d=> startTime.plus(d.toMillis))
    }
    new SettableClock(startTime, timeScale, endTime, stopped = true)
  }

  def wallClockStartTime (isLaunched: Boolean): Option[DateTime] = {
    config.getOptionalDateTime("start-at") match {
      case absDateOpt @ Some(date) => absDateOpt // value is wallclock time
      case None =>
        if (isLaunched) config.getOptionalFiniteDuration("start-in").map(DateTimeUtils.fromNow)
        else None
    }
  }

  def scheduleStart(date: DateTime) = {
    info(s"scheduling start of universe $name at $date")
    val dur = FiniteDuration(date.getMillis - System.currentTimeMillis(), MILLISECONDS)
    system.scheduler.scheduleOnce(dur, new Runnable {
      override def run: Unit = {
        startActors
      }
    })
  }
  def scheduleTermination(date: DateTime) = {
    info(s"scheduling termination of universe $name at $date")
    val dur = FiniteDuration(date.getMillis - System.currentTimeMillis(), MILLISECONDS)
    system.scheduler.scheduleOnce(dur, new Runnable {
      override def run: Unit = terminate
    })
  }

  def createRaceContext(master: ActorRef, bus: Bus): RaceContext = RaceContext(master, bus)

  def getActorConfigs = config.getOptionalConfigList("actors")

  def createActors = {
    info(s"creating actors of universe $name ..")
    askVerifiableForResult(master, RaceCreate) {
      case RaceCreated =>
        status = Created
        info(s"universe $name created")
      case RaceCreateFailed(reason) => error(s"creating universe $name failed with: $reason")
      case TimedOut => error(s"creating universe $name timed out")
      case e => error(s"invalid response creating universe $name: $e")
    }(createTimeout)
  }

  def initializeActors = {
    info(s"initializing actors of universe $name ..")
    askVerifiableForResult(master, RaceInitialize) {
      case RaceInitialized =>
        status = Initialized
        info(s"universe $name initialized")
      case RaceInitializeFailed(reason) => abort(s"initializing universe $name failed with: $reason")
      case TimedOut => abort(s"initializing universe $name timed out")
      case e => abort(s"invalid response initializing universe $name: $e")
    }(initTimeout)
  }

  def abort (msg: String): Unit = {
    error(msg)
    terminate
  }

  // called by RACE driver (TODO enforce or verify)
  def startActors = {
    if (status == Initialized) {
      ifSome(simClock.wallEndTime) { scheduleTermination }

      info(s"starting actors of universe $name ..")
      status = Started
      askVerifiableForResult(master, RaceStart) {
        case RaceStarted =>
          status = Running
          info(s"universe $name running")
        case RaceStartFailed(reason) => abort(s"starting universe $name failed: $reason")
        case TimedOut => abort(s"starting universe $name timed out")
        case e => abort(s"invalid response starting universe $name: $e")
      }(startTimeout)
    } else warning(s"universe $name cannot be started in state $status")
  }

  def stoppedRaceActor(actorRef: ActorRef): Unit = {
    info(s"unregister stopped ${actorRef.path}")
    actors = actors - actorRef
  }

  /**
    * graceful shutdown that synchronously processes terminateRaceActor() actions
    * called by RACE driver (TODO enforce or verify)
    */
  def terminate: Boolean = {
    if (isLive) {
      info(s"universe $name terminating..")
      status = Terminating
      askVerifiableForResult(master, RaceTerminate) {
        case RaceTerminated =>
          raceTerminated
          true
        case TimedOut =>
          warning(s"universe $name termination timeout")
          false
      }
    } else { // nothing to shut down
      true
    }
  }

  def isTerminating = status == Terminating
  def isLive = status != Terminating && status != gov.nasa.race.common.Status.Terminated

  def currentStatus = status

  // internal (overridable) method to clean up *after* successful termination of all actors
  protected def raceTerminated: Unit = {
    info(s"universe $name terminated")
    status = common.Status.Terminated
    RaceLogger.terminate(system)

    if (!isRunningEmbedded) system.terminate  // don't terminate if running in a MultiJvm test
  }
  // some actor asked for termination
  def terminationRequest(actorRef: ActorRef) = {
    if (!isTerminating) { // avoid recursive termination
      if ((allowSelfTermination && isManagedActor(actorRef)) ||
        (allowRemoteTermination && isRemoteActor(actorRef))) terminate
      else warning(s"universe ignoring termination request from ${actorRef.path}")
    }
  }

  /**
    * some actor asks for a simClock reset
    * TODO - this does not yet handle remote RAS
    */
  def requestSimClockReset(requester: ActorRef, date: DateTime, tScale: Double): Boolean = {
    if (isLive) {
      if (commonCapabilities.supportsSimTimeReset) {
        info(s"sim clock reset on behalf of ${requester.path.name} to ($date,$tScale)")
        simClock.reset(date, tScale)
        master ! RaceResetClock(requester,date,tScale)
        true
      } else {
        warning(s"universe $name rejected sim clock reset (insufficient actor capabilities)")
        false
      }
    } else false // nothing to reset
  }

  def setTimescale (tScale: Double): Boolean = {
    if (isLive) {
      if (commonCapabilities.supportsSimTimeReset) {
        info(s"set sim clock timescale to: $tScale")
        val date = simClock.dateTime
        simClock.reset(date,tScale)
        master ! RaceResetClock(ActorRef.noSender, date, tScale)
        true
      } else {
        warning("setting time scale ignored (insufficient actor capabilities)")
        false
      }
    } else {
      warning(s"universe $name is not live")
      false
    }
  }

  def pauseResume: Boolean = {
    if (isLive) {
      if (commonCapabilities.supportsSimTimeReset) {
        if (simClock.isStopped) {
          info("resuming sim clock")
          simClock.resume
        } else {
          info("stopping sim clock")
          simClock.stop
        }
        master ! RaceResetClock(ActorRef.noSender, simClock.dateTime, simClock.timeScale)
        true
      } else {
        warning("pause/resume ignored (insufficient actor capabilities)")
        false
      }
    } else {
      warning(s"universe $name is not live")
      false
    }
  }

  def isStopped = simClock.isStopped

  /**
    * check if we accept connection requests from this remote master, i.e. some of our local actors are used
    * as its remote actors. In case the request is accepted, store the master in `usingRemoteMasters`
    *
    * @return true if request is accepted
    */
  def acceptsRemoteConnectionRequest (remoteMaster: ActorRef): Boolean = {
    // TODO check against config if we accept a connection request from this remote master
    // this can filter based on network (ip address) and universe name
    usingRemoteMasters = usingRemoteMasters + remoteMaster
    true
  }

  def isKnownRemoteMaster (actorRef: ActorRef) = usingRemoteMasters.contains(actorRef)

  final val systemPrefix = s"akka://$name" // <2do> check managed remote actor paths
  def isRemoteActor(actorRef: ActorRef) = !actorRef.path.toString.startsWith(systemPrefix)

  def isManagedActor(actorRef: ActorRef) = actors.contains(actorRef)

  /**
   * hard shutdown command issued from outside the RaceActorSystem
   * NOTE - this might loose data since actors are not processing their terminateRaceActor()
   */
  def kill = {
    info(s"universe $name killed")
    status = common.Status.Terminated // there is no coming back from here
    RaceLogger.terminate(system)
    system.terminate
  }

  override def logLevel: LogLevel = RaceLogger.getConfigLogLevel(system,
    config.getOptionalString("loglevel")).getOrElse(system.eventStream.logLevel)

  override def setLogLevel(logLevel: LogLevel) = {
    system.eventStream.setLogLevel(logLevel)
    master ! SetLogLevel(logLevel)
  }

  //--- message injection

  def publish(channel: String, msg: Any) = bus.publish(BusEvent(channel, msg, master))

  def send(path: String, msg: Any) = system.actorSelection(path) ! msg

  //--- state query & display

  // NOTE - this performs a sync query
  def showActors = {
    for (((actorRef, _), i) <- actors.zipWithIndex) {
      print(f"  $i%2d: ${actorRef.path} ")

      askForResult(actorRef ? PingRaceActor()) {
        case PingRaceActor(tSent, tReceived) =>
          val tReturned = System.nanoTime()
          println(f"  ${tReceived - tSent}ns / ${tReturned - tReceived}ns")
        case TimedOut => println("  TIMEOUT")
        case other => println("  INVALID RESPONSE")
      }
    }
  }

  def showChannels = {
    println(bus.showChannelSubscriptions)
  }

}
