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

package gov.nasa.race.actor

import com.typesafe.config.Config

/**
  * actor that publishes to either one of two channels based on configured filtering
  */
class EitherOrRouter (val config: Config) extends FilteringPublisher {
  val writeToPass = config.getString("write-to-pass")
  val writeToFail = config.getString("write-to-fail")

  override def action (msg: Any, isPassing: Boolean) = {
    if (isPassing) publish(writeToPass, msg)
    else publish(writeToFail, msg)
  }
}