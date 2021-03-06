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
package gov.nasa.race.util

import java.io.ByteArrayOutputStream
import java.lang.reflect.Method

/**
  * java.lang.Class related utility functions
  */
object ClassUtils {

  def getResourceAs[T] (cls: Class[_], name: String)(f: (ByteArrayOutputStream) => T): Option[T] = {
    val is = cls.getResourceAsStream(name)
    if (is != null) {
      val len = is.available()
      val buf = new Array[Byte](len)
      val bao = new ByteArrayOutputStream
      is.read(buf)
      bao.write(buf)
      is.close
      Some(f(bao))
    } else None
  }

  def getResourceAsString(cls: Class[_], name: String): Option[String] = {
    getResourceAs(cls,name){ _.toString("UTF-8") }
  }

  def getResourceAsBytes(cls: Class[_], name: String): Option[Array[Byte]] = {
    getResourceAs(cls,name){ _.toByteArray }
  }

  def getMethod(cls: Class[_], name: String, argTypes: Class[_]*): Option[Method] = {
    try {
      Some(cls.getMethod(name,argTypes:_*))
    } catch {
      case x:NoSuchMethodException => None
    }
  }
}
