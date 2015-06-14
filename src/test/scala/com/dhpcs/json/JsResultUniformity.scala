/*
 * Copyright (C) 2015 David Piggott <https://www.dhpcs.com>
 */
package com.dhpcs.json

import org.scalactic.Uniformity
import play.api.libs.json.{JsError, JsResult}

class JsResultUniformity[A] extends Uniformity[JsResult[A]] {

  override def normalizedCanHandle(b: Any) = b match {
    case _: JsError => true
    case _ => false
  }

  override def normalizedOrSame(b: Any) = b match {
    case jsError: JsError => normalized(jsError)
    case _ => b
  }

  override def normalized(aJsResult: JsResult[A]) = aJsResult match {
    case jsError: JsError =>
      jsError.copy(
        errors = jsError.errors.sortBy { case (jsPath, _) => jsPath.toJsonString }
      )
    case _ => aJsResult
  }

}
