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

package gov.nasa.race.common

import com.github.nscala_time.time.Imports._

import scala.concurrent.duration._


/**
  * time keeping utility that can be used to track simulation time, supporting
  * initial value and scaling
  * Note that we don't use vals since derived classes can change the values, and we don't use
  * public vars since each change should go through such a derived class
  */
class Clock (initTime: DateTime = DateTime.now,
             initTimeScale: Double = 1,
             endTime: Option[DateTime] = None,
             stopped: Boolean=false)
         extends Cloneable {

  protected var _timeScale = initTimeScale
  protected var _base = initTime // sim time
  protected var _end = endTime
  protected var _initMillis = System.currentTimeMillis  // wall time
  protected var _stoppedAt: Long = if (stopped) _initMillis else 0 // wall time

  def currentMillis = if (_stoppedAt > 0) _stoppedAt else System.currentTimeMillis

  @inline def timeScale: Double = _timeScale
  def base = _base
  def end = _end
  def initMillis = _initMillis
  @inline def stoppedAt = _stoppedAt
  def baseMillis = _base.getMillis
  def endMillis = endTimeMillis

  @inline def isStopped = _stoppedAt != 0


  /** simulation time milliseconds */
  @inline def millis: Long = _base.getMillis + ((currentMillis - _initMillis) * _timeScale).toLong

  /** simulation time DateTime */
  def dateTime: DateTime = _base + ((currentMillis - _initMillis) * _timeScale).toLong

  /** simulation time duration since initTime */
  def elapsed: FiniteDuration = ((currentMillis - _initMillis) * _timeScale).toLong.milliseconds

  /** wallclock time duration since initTime */
  @inline def elapsedWall: FiniteDuration = (currentMillis - _initMillis).milliseconds

  /** wallclock time for given sim time */
  def wallTime (simTime: DateTime): DateTime = {
    DateTime.now.plusMillis(((simTime.getMillis - millis)/_timeScale).toInt)
  }

  /** wallclock time for given sim duration */
  def wallTimeIn (simDuration: FiniteDuration): DateTime = {
    DateTime.now.plusMillis((simDuration.toMillis/_timeScale).toInt)
  }

  def wallEndTime: Option[DateTime] = _end.map(wallTime)

  def endTimeMillis: Long = _end match {
    case Some(date) => date.getMillis
    case None => Long.MaxValue
  }

  def exceedsEndTime (d: DateTime): Boolean = d.getMillis > endTimeMillis

  def save = clone.asInstanceOf[Clock]
}

/**
  * a Clock that can be controlled at runtime, supporting reset and stop/resume
  */
class SettableClock (initTime: DateTime = DateTime.now,
                     initTimeScale: Double = 1,
                     endTime: Option[DateTime] = None,
                     stopped: Boolean = false) extends Clock (initTime,initTimeScale,endTime,stopped) {

  def reset (initTime: DateTime, initTimeScale: Double = 1, endTime: Option[DateTime] = None): SettableClock = synchronized {
    _timeScale = initTimeScale
    _base = initTime
    _end = endTime
    _initMillis = System.currentTimeMillis
    if (_stoppedAt > 0) _stoppedAt = _initMillis
    this
  }

  def reset (saved: Clock): SettableClock = synchronized {
    _timeScale = saved.timeScale
    _base = saved.base
    _end = saved.end
    _initMillis = saved.initMillis
    _stoppedAt = saved.stoppedAt
    this
  }

  def stop = synchronized { _stoppedAt = System.currentTimeMillis }
  def resume = synchronized { _stoppedAt = 0 }
}
