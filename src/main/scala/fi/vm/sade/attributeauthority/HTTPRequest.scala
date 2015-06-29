package fi.vm.sade.attributeauthority

import scala.collection.immutable.HashMap

trait HttpRequest{
  def responseWithHeaders(): (Int, Map[String, String], String)
  def response(): Option[String]
  def param(key: String, value: String): HttpRequest
  def header(key: String, value: String): HttpRequest
}

class DefaultHttpRequest(private val request: scalaj.http.HttpRequest) extends HttpRequest {
  def param(key: String, value: String) = {
    new DefaultHttpRequest(request.param(key, value))
  }

  def header(key: String, value: String) = {
    new DefaultHttpRequest(request.header(key, value))
  }

  def responseWithHeaders(): (Int, Map[String, String], String) = {
    try {
      val response = request.asString
      (response.code, response.headers, response.body)
    } catch {
      case t: Throwable => {
        logUnexpectedError(t)
        (500, HashMap(), "")
      }
    }
  }

  def response(): Option[String] = {
    try {
      val response = request.asString
      if (response.code == 404)
        None
      else
        Some(request.asString.body)
    } catch {
      case t: Throwable => {
        logUnexpectedError(t)
        None
      }
    }
  }

  private def logUnexpectedError(t: Throwable) {
    println("Unexpected error from " + request.method + " to " + request.url + " : " + t, t)
  }
}

