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

package gov.nasa.race.ww.track

import java.awt.Point

import gov.nasa.race._
import gov.nasa.race.track.TrackedObject
import gov.nasa.race.util.DateTimeUtils._
import gov.nasa.race.ww._
import gov.nasa.race.ww.Implicits._
import gov.nasa.worldwind.WorldWind
import gov.nasa.worldwind.avlist.AVKey
import gov.nasa.worldwind.render.{MultiLabelPointPlacemark, Offset, PointPlacemarkAttributes}

object TrackRenderLevel extends Enumeration {
  type TrackRenderLevel = Value
  val Symbol, Label, Dot = Value
}
import gov.nasa.race.ww.track.TrackRenderLevel._

/**
  * Renderable representing 2D aircraft symbol
  *
  * Note this is a pick-enabled object type and hence we need a link to the corresponding
  * FlightEntry (the FlightPos objects referenced from there change upon update and hence
  * cannot be used to do a FlightEntry lookup)
  */
class TrackSymbol[T <: TrackedObject](val trackEntry: TrackEntry[T])
                                                  extends MultiLabelPointPlacemark(trackEntry.obj) with RaceLayerPickable {
  // RaceLayerObject
  override def layer = trackEntry.layer
  override def layerItem = trackEntry

  var showDisplayName = false
  var attrs = new PointPlacemarkAttributes

  //--- those are invariant
  attrs.setLabelColor(layer.labelColorString)
  attrs.setLabelFont( layer.labelFont)
  attrs.setLineColor(layer.lineColor)
  //attrs.setImageColor(layer.color) // does not seem to work, and if so would probably require monochrome images

  ifSome(layer.subLabelFont) { f=> setSubLabelFont(f) }

  setAltitudeMode(WorldWind.ABSOLUTE)
  //updateDisplayName
  updateAttributes

  def setDisplayName(showIt:Boolean) = {
    if (showIt) {
      showDisplayName = true
      updateDisplayName
    } else showDisplayName = false
  }

  def updateDisplayName = {
    val obj = trackEntry.obj
    val s = s"${obj.cs}\n${hhmmss.print(obj.date)}\n${obj.altitude.toFeet.toInt} ft\n${obj.heading.toDegrees.toInt}°\n${obj.speed.toKnots.toInt} kn"
    setValue( AVKey.DISPLAY_NAME, s.toString)
  }

  def update (newT: T) = {
    setPosition(newT)
    attrs.setHeading(newT.heading.toDegrees)

    if (hasLabel) layer.updateLabel(this)
    if (showDisplayName) updateDisplayName
    //updateAttributes
  }

  def setDotAttrs = {
    removeAllLabels()
    attrs.setScale(5d)
    attrs.setImage(null)
    attrs.setUsePointAsDefaultImage(true)
    setAttributes(attrs)
  }

  def setLabelAttrs = {
    layer.setLabel(this)
    attrs.setScale(5d)
    attrs.setImage(null)
    attrs.setUsePointAsDefaultImage(true)
    setAttributes(attrs)
  }

  def setSymbolAttrs = {
    val obj = trackEntry.obj

    layer.setLabel(this)
    attrs.setImage(layer.image(obj))
    attrs.setScale(0.35)
    attrs.setImageOffset(Offset.CENTER)
    attrs.setHeading(obj.heading.toDegrees)
    attrs.setHeadingReference(AVKey.RELATIVE_TO_GLOBE)
    setAttributes(attrs)
  }

  def updateAttributes = {
    layer.trackDetails match {
      case Dot => setDotAttrs
      case Label => setLabelAttrs
      case Symbol => setSymbolAttrs
    }
  }

  val screenPt = if (screenPoint != null) new Point(screenPoint.x.toInt, screenPoint.y.toInt) else new Point(0,0)
  def getScreenPoint: Point = {
    if (screenPoint != null) screenPt.setLocation(screenPoint.x, screenPoint.y)
    screenPt
  }

  val screenOffset = if (screenPoint != null) new Offset(screenPoint.x,screenPoint.y,AVKey.PIXELS,AVKey.PIXELS) else new Offset(.0,.0,AVKey.PIXELS,AVKey.PIXELS)
  def getScreenOffset: Offset = {
    if (screenPoint != null){ screenOffset.setX(screenPoint.x); screenOffset.setY(screenPoint.y) }
    screenOffset
  }
  def screenPointX = screenPoint.x
  def screenPointY = screenPoint.y
}
