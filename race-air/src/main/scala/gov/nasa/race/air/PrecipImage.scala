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

import java.awt.image.BufferedImage

import com.github.nscala_time.time.Imports._
import com.typesafe.config.{Config, ConfigValue, ConfigValueFactory}
import gov.nasa.race.config.{ConfigValueMapper, ConfigurableTimeTranslator}
import gov.nasa.race.geo.LatLonPos
import gov.nasa.race.uom._

import scala.collection.concurrent.TrieMap

object PrecipImageStore {
  // if we would have two images of the same product type and trp/offset their grids would overlap
  def computeId (product: Int, site: String, xoffset: Length, yoffset: Length) =
    s"$site-$product-${xoffset.toKilometers}-${yoffset.toKilometers}"

  val imageStore = TrieMap[String,BufferedImage]()
}

/**
 * precipitation image created from ITWS precip messages
 */
case class PrecipImage (id: String, product: Int, site: String, genDate: DateTime, expDate: DateTime,
                        trpPos: LatLonPos, xoffset: Length, yoffset: Length, rotation: Angle,
                        width: Length, height: Length,
                        maxPrecipLevel: Int,
                        img: BufferedImage) {

  override def toString = s"PrecipImage{$site-$product, ${genDate.toString("yy/MM/dd HH:mm:ss")} to ${expDate.toString("HH:mm:ss")}, max:$maxPrecipLevel"

  def copyImage: BufferedImage = {
    val cm = img.getColorModel
    val isAlphaPremultiplied = cm.isAlphaPremultiplied
    val raster = img.copyData(img.getRaster.createCompatibleWritableRaster)
    new BufferedImage(cm, raster, isAlphaPremultiplied, null)
  }
}

class PrecipImageTimeAdjuster (val config: Config = null) extends ConfigurableTimeTranslator {
  override def translate (obj: Any, simTime: DateTime) = {
    obj match {
      case pi: PrecipImage =>
        val dt = (pi.expDate.getMillis - pi.genDate.getMillis)
        val newGenDate = simTime
        val newExpDate = simTime + dt
        pi.copy( genDate = newGenDate, expDate = newExpDate)
      case other => other
    }
  }
}


class PrecipImage2Product (val config: Config=null) extends ConfigValueMapper {
  def translate (obj: Any): Option[ConfigValue] = {
    obj match {
      case pi: PrecipImage => Some(ConfigValueFactory.fromAnyRef(pi.product))
      case other => None
    }
  }
}