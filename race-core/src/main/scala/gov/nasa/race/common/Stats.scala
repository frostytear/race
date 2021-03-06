/*
 * Copyright (c) 2017, United States Government, as represented by the
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

import java.io.PrintWriter

import scala.xml.NodeSeq

/**
  * the generic container for statistics. This can be sent as a message, as long as
  * the snapshot element type is serializable
  */
trait Stats extends Cloneable with XmlSource {
  val topic: String         // what we measure
  val source: String        // where we measure it from
  val takeMillis: Long      // absolute time value when stats snapshot was taken
  val elapsedMillis: Long   // duration covered by the snapshot

  //--- XML generation interface
  def toXML = {
    <stats topic={topic} source={source} takeMillis={takeMillis.toString} elapsedMillis={elapsedMillis.toString}>
      {xmlData}
    </stats>
  }
  // override for stats child elements
  def xmlData: xml.NodeSeq = NodeSeq.Empty
}

/**
  * Stats that can be printed
  */
trait PrintStats extends Stats {
  def printWith(pw: PrintWriter)
}

/**
  * a formatter that prints Stats (for configurable reporters)
  */
trait PrintStatsFormatter {
  def printWith(pw: PrintWriter, stats: Stats): Boolean // return true if stats were written
}


/**
  * generic pattern match statistics
  */
class PatternStatsData(val pattern: String) extends XmlSource with Cloneable {
  var count: Int = 0

  def snapshot = super.clone.asInstanceOf[PatternStatsData]

  override def toXML = {
    <match>
      <pattern>{pattern}</pattern>
      <count>{count}</count>
    </match>
  }
}
