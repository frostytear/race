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
package gov.nasa.race.air

import gov.nasa.race._
import gov.nasa.race.common.XmlParser
import gov.nasa.race.track._
import gov.nasa.race.util.XmlAttrProcessor
import org.joda.time.DateTime

import scala.collection.mutable.ArrayBuffer

/**
  * a XML parser that creates and stores TrackInfos from fdm:fltdMessage messages
  */
class TFMTrackInfoParser extends XmlParser[Seq[TrackInfo]] with XmlAttrProcessor {
  setBuffered(4096)

  val tInfos = new ArrayBuffer[TrackInfo]

  onStartElement = {
    case "ds:tfmDataService" => tfmDataService // the top element (message) we parse
    case other => stopParsing
  }

  def tfmDataService = {
    tInfos.clear
    whileNextElement {
      case "fdm:fltdMessage" =>
        fltdMessage
      case _ => // ignore
    } {
      case "ds:tfmDataService" =>
        if (tInfos.nonEmpty) setResult(tInfos)
      case other => // ignore
    }
  }

  def fltdMessage: Unit = {
    var cs: String = null
    var departurePoint, arrivalPoint: String = null
    var trackCat, trackType: String = null
    var etd,atd,eta,ata: DateTime = null
    var lat = Double.NaN
    var lon = Double.NaN
    var route: Trajectory = EmptyTrajectory

    val trackRef = readAttribute("flightRef")

    whileNextElement {
      case "nxce:aircraftId" =>
        cs = trimmedTextOrNull
      case "nxce:airport" =>
        if (hasParent("nxce:departurePoint")) departurePoint = trimmedTextOrNull()
        else if (hasParent("nxce:arrivalPoint")) arrivalPoint = trimmedTextOrNull()
      case "nxcm:etd" =>  // "actual" estimates?
        parseTime("etdType", etd = _, atd = _)
      case "nxcm:eta" =>
        parseTime("etaType", eta = _, ata = _)
      case "nxcm:newFlightAircraftSpecs" =>
        trackCat = attributeOrNull("specialAircraftQualifier")
        trackType = trimmedTextOrNull
      case "nxce:waypoint" =>
        lat = Double.NaN
        lon = Double.NaN
        processAttributes {
          case "latitudeDecimal" => lat = doubleValue
          case "longitudeDecimal" => lon = doubleValue
        }
      case _ => // ignore
    } {
      case "nxce:waypoint" =>
        if (!lat.isNaN && !lon.isNaN) {
          route = route.add(lat,lon,Double.NaN,0) // no altitude or time info
        }

      case "fdm:fltdMessage" =>
        val ti = new TrackInfo( trackRef, cs,
                         optional(trackCat), optional(trackType),
                         optional(departurePoint), optional(arrivalPoint),
                         optional(etd), optional(atd), optional(eta), optional(ata),
                         if (route.nonEmpty) Some(route) else None)
        tInfos += ti
        return
      case _ => // ignore
    }
  }

  def parseTime (tAttr: String, estimateAction: DateTime=>Unit, actualAction: DateTime=>Unit) = {
    var tType: String = null
    processAttributes {
      case `tAttr` => tType = value
      case "timeValue" => if (value != null){
        val date = DateTime.parse(value)
        tType match {
          case "ESTIMATED" => estimateAction(date)
          case "ACTUAL" => actualAction(date)
          case _ => // no or unknown type
        }
      }
    }
  }
}