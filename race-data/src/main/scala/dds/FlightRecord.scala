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

package gov.nasa.race.data.dds

import com.typesafe.config.Config
import org.joda.time.DateTime
import gov.nasa.race.common.DateTimeUtils._
import gov.nasa.race.common.{ConfigurableTranslator, DDSReader, DDSWriter, Show}
import gov.nasa.race.data.{FlightPos, LatLonPos}
import squants.motion.Knots
import squants.space.{Degrees, Feet}

import scala.language.implicitConversions

/**
  * companion object for the automatically generated FlightRecord class
  * this is an example of how to use Scala implicits to add methods for IDL defined data, which are just structs
  */
object FlightRecord {

  def show (fr: dds.FlightRecord): String = {
    val d = new DateTime(fr.date)
    f"FlightRecord{cs='${fr.cs}%s',date=${hhmmss.print(d)},lat=${fr.lat}%.6f,lon=${fr.lon}%.6f,alt=${fr.alt}%.1f,heading=${fr.heading}%.1f,speed=${fr.speed}%.1f}"
  }

  implicit class RichFlightRecord (fr: dds.FlightRecord) extends Show {
    def show: String = FlightRecord.show(fr)
  }

  implicit def fpos2Fr (fpos: FlightPos): dds.FlightRecord = {
    val pos = fpos.position
    new dds.FlightRecord(fpos.cs, pos.φ.toDegrees, pos.λ.toDegrees,
      fpos.altitude.toFeet, fpos.speed.toKnots, fpos.heading.toDegrees, fpos.date.getMillis)
  }

  implicit def fr2Fpos (fr: dds.FlightRecord): FlightPos = {
    FlightPos("?",fr.cs,LatLonPos.fromDegrees(fr.lat,fr.lon),
      Feet(fr.alt),Knots(fr.speed),Degrees(fr.heading),new DateTime(fr.date))
  }
}
import FlightRecord._

//--- DDS read/write support

/**
  * a DDSReader for dds.FlightRecord data
  */
class FlightRecordReader (val config: Config) extends DDSReader[dds.FlightRecord](config)

/**
  * a DDSWriter for FlightRecords
  */
class FlightRecordWriter (val config: Config) extends DDSWriter[dds.FlightRecord](config) {
  override def write (o: Any) = {
    o match {
      case fr: dds.FlightRecord => writer.write(fr)
      case fpos: FlightPos => writer.write(fpos)
      case other => // ignored for now
    }
  }
}

//--- translation to/from DDS types (which are just structs)

class FlightPos2FlightRecord (val config: Config=null) extends ConfigurableTranslator {
  override def translate(src: Any): Option[Any] = {
    src match {
      case fpos: FlightPos => Some(FlightRecord.fpos2Fr(fpos))
      case other => None
    }
  }
}

class FlightRecord2FlightPos (val config: Config=null) extends ConfigurableTranslator {
  override def translate(src: Any): Option[Any] = {
    src match {
      case fr: dds.FlightRecord => Some(FlightRecord.fr2Fpos(fr))
      case other => None
    }
  }
}