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

import gov.nasa.race.test.RaceSpec
import gov.nasa.race.util.FileUtils
import org.scalatest.FlatSpec

/**
  * regression test for XmlValidationFilter
  */
class XmlValidationFilterSpec extends FlatSpec with RaceSpec {

  "a XmlValidationFilter" should "reject a message that does not conform to a schema" in {
    val schemaFile = baseResourceFile("schema.xsd")
    val validationFilter = new XmlValidationFilter(schemaFile)
    val invalidMessage = FileUtils.fileContentsAsUTF8String(baseResourceFile("invalid.xml"))

    println("invalid.xml should be rejected...")
    validationFilter.pass(invalidMessage) shouldBe false
    println("Ok")
  }

  "a XmlValidationFilter" should "let pass a message that does conform to a schema" in {
    val schemaFile = baseResourceFile("schema.xsd")
    val validationFilter = new XmlValidationFilter(schemaFile)
    val validMessage = FileUtils.fileContentsAsUTF8String(baseResourceFile("valid.xml"))

    print("valid.xml should pass...")
    validationFilter.pass(validMessage) shouldBe true
    println("Ok")
  }
}
