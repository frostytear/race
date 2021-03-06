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

import gov.nasa.race.geo.LatLonPos
import gov.nasa.race.test.RaceSpec
import gov.nasa.race.track.CompactTrajectory
import org.joda.time.DateTime
import org.scalatest.FlatSpec
import gov.nasa.race.uom.Length._
import gov.nasa.race.uom.Angle._
import gov.nasa.race.uom.Speed._

/**
  * unit test for CompactFlightPath
  */
class CompactFlightPathSpec extends FlatSpec with RaceSpec {

  behavior of "CompactFlightPath"

  "path object" should "reproduce input values" in {
    val positions = Array[(Double,Double,Double,DateTime)](
      ( 37.57251,-122.26521, 1475 , DateTime.parse("2016-07-03T13:53:17.135") ),
      ( 37.57301,-122.26643, 1450 , DateTime.parse("2016-07-03T13:53:18.574") ),
      ( 37.57320,-122.26685, 1450 , DateTime.parse("2016-07-03T13:53:19.098") ),
      ( 37.57333,-122.26720, 1450 , DateTime.parse("2016-07-03T13:53:19.549") ),
      ( 37.57352,-122.26766, 1450 , DateTime.parse("2016-07-03T13:53:20.080") ),
      ( 37.57484,-122.27070, 1400 , DateTime.parse("2016-07-03T13:53:23.812") ),
      ( 37.57535,-122.27193, 1375 , DateTime.parse("2016-07-03T13:53:25.325") ),
      ( 37.57549,-122.27228, 1375 , DateTime.parse("2016-07-03T13:53:25.776") ),
      ( 37.57599,-122.27345, 1350 , DateTime.parse("2016-07-03T13:53:27.283") ),
      ( 37.57650,-122.27461, 1325 , DateTime.parse("2016-07-03T13:53:28.727") ),
      ( 37.57663,-122.27497, 1325 , DateTime.parse("2016-07-03T13:53:29.184") ),
      ( 37.57711,-122.27610, 1300 , DateTime.parse("2016-07-03T13:53:30.566") ),
      ( 37.57729,-122.27652, 1300 , DateTime.parse("2016-07-03T13:53:31.093") ),
      ( 37.57745,-122.27690, 1275 , DateTime.parse("2016-07-03T13:53:31.614") ),
      ( 37.57827,-122.27891, 1250 , DateTime.parse("2016-07-03T13:53:34.172") ),
      ( 37.57846,-122.27932, 1225 , DateTime.parse("2016-07-03T13:53:34.755") ),
      ( 37.57864,-122.27976, 1225 , DateTime.parse("2016-07-03T13:53:35.286") ),
      ( 37.57883,-122.28023, 1225 , DateTime.parse("2016-07-03T13:53:35.870") ),
      ( 37.57911,-122.28094, 1200 , DateTime.parse("2016-07-03T13:53:36.790") ),
      ( 37.57928,-122.28134, 1200 , DateTime.parse("2016-07-03T13:53:37.313") ),
      ( 37.57942,-122.28163, 1200 , DateTime.parse("2016-07-03T13:53:37.705") ),
      ( 37.57957,-122.28207, 1175 , DateTime.parse("2016-07-03T13:53:38.297") ),
      ( 37.57976,-122.28255, 1175 , DateTime.parse("2016-07-03T13:53:38.885") ),
      ( 37.58011,-122.28327, 1175 , DateTime.parse("2016-07-03T13:53:39.871") ),
      ( 37.58057,-122.28438, 1150 , DateTime.parse("2016-07-03T13:53:41.313") ),
      ( 37.58070,-122.28479, 1125 , DateTime.parse("2016-07-03T13:53:41.840") ),
      ( 37.58088,-122.28524, 1125 , DateTime.parse("2016-07-03T13:53:42.428") ),
      ( 37.58106,-122.28559, 1125 , DateTime.parse("2016-07-03T13:53:42.951") ),
      ( 37.58116,-122.28590, 1125 , DateTime.parse("2016-07-03T13:53:43.346") ),
      ( 37.58134,-122.28631, 1100 , DateTime.parse("2016-07-03T13:53:43.871") )
    )

    val path = new CompactTrajectory(2)  // force growth during population
    for (e <- positions) {
      val fpos = new FlightPos("123","X42", LatLonPos.fromDegrees(e._1,e._2), Meters(e._3), Knots(100.0),Degrees(42), e._4)
      path.add(fpos)
    }

    path.foreach { (i, latDeg, lonDeg, altMeters, tMillis) =>
      println(f"$i%2d :  $latDeg%.5f,$lonDeg%.5f, $altMeters%.0f, ${new DateTime(tMillis)}")
      val e = positions(i)
      latDeg should be(e._1 +- 0.000001)
      lonDeg should be(e._2 +- 0.000001)
      altMeters should be(e._3 +- 0.01)
      tMillis should be(e._4.getMillis)
    }
  }
}


