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

import gov.nasa.race.config.ConfigUtils._
import java.io.{BufferedOutputStream, File, FileOutputStream, OutputStream}
import java.util.zip.GZIPOutputStream

import akka.actor.ActorRef
import com.typesafe.config.Config

/**
  * a RaceActor that continuously writes to a configurable file
  * used by ArchiveActor etc.
  */
trait FileWriterRaceActor extends RaceActor {
  val config: Config

  def defaultPathName = s"tmp/$name" // override in concrete class
  val pathName = config.getStringOrElse("pathname", defaultPathName)

  var compressedMode = config.getBooleanOrElse("compressed", false)
  val appendMode = config.getBooleanOrElse("append", false)
  val bufSize = config.getIntOrElse("buffer-size", 4096)

  if (compressedMode && appendMode) {
    warning(s"$name cannot append to compressed stream, disabling compression")
    compressedMode = false
  }

  val oStream = openStream

  def openStream: OutputStream = {
    val pn = if (compressedMode) pathName + ".gz" else pathName
    val file = new File(pn)
    val dir = file.getParentFile
    if (!dir.isDirectory) dir.mkdirs()

    val fs = new FileOutputStream(file, appendMode)
    if (compressedMode) new GZIPOutputStream(fs,bufSize) else new BufferedOutputStream( fs, bufSize)
  }

  override def onTerminateRaceActor(originator: ActorRef) = {
    oStream.flush()
    oStream.close()
    super.onTerminateRaceActor(originator)
  }

  def write (s: String): Unit = oStream.write(s.getBytes)
  def write (ba: Array[Byte]): Unit = oStream.write(ba)
  def writeln = oStream.write('\n')
}
