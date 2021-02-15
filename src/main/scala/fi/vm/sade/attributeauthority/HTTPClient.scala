package fi.vm.sade.attributeauthority

import scalaj.http.HttpOptions.HttpOption
import scalaj.http.{Http, HttpOptions}

trait HttpClient {
  def httpGet(url: String) : HttpRequest
  def httpGet(url: String, options: HttpOption*): HttpRequest
  def httpPost(url: String, data: Option[String]) : HttpRequest
}

object DefaultHttpClient extends HttpClient {
  def httpGet(url: String) : HttpRequest = {
    new DefaultHttpRequest(modifyRequest(Http(url)))
  }

  def httpGet(url: String, options: HttpOption*): HttpRequest = {
    new DefaultHttpRequest(modifyRequest(Http(url)).options(options.toList))
  }

  def httpPost(url: String, data: Option[String]) : HttpRequest = {
    new DefaultHttpRequest(modifyRequest(data match {
      case None => Http(url).postForm
      case Some(data) => Http(url).postData(data)
    }))
  }

  private def modifyRequest(request: scalaj.http.HttpRequest): scalaj.http.HttpRequest = {
    request
      .option(HttpOptions.connTimeout(5000))
      .option(HttpOptions.readTimeout(10000))
      .option(HttpOptions.followRedirects(true))
      .header("Caller-id", AppConfig.callerId)
      .header("CSRF", AppConfig.callerId)
  }
}