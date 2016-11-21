package fi.vm.sade.attributeauthority

import java.util.concurrent.TimeUnit

import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.concurrent.duration.Duration
import scalaj.http.HttpOptions.HttpOption

class RemoteAuthenticationInfoServiceSpec extends ScalatraFunSuite {
  def createMock() = new Mocks(
    Map(
      ("POST", "http://localhost/cas/v1/tickets") ->
        MockResponse(201, Map("Location" -> "http://localhost/cas/v1/tickets/TGT-63528-7e6K4Ft6YCbiiLFdjk7Y-cas.foo"), ""),

      ("POST", "http://localhost/cas/v1/tickets/TGT-63528-7e6K4Ft6YCbiiLFdjk7Y-cas.foo") ->
        MockResponse(200, Map(), "", Some("ST-63529-TQpkXpIE0YgGXFIwTHaj-cas.foo")),

      ("GET", "http://localhost/authentication-service/j_spring_cas_security_check") ->
        MockResponse(200, Map("Set-Cookie" -> "JSESSIONID=9C16A50F8E5DE52D03F237CB3500D3A8"), ""),

      ("GET", "http://localhost/authentication-service/resources/s2s/byHetu/111111-1975") ->
        MockResponse(200, Map("Content-Type" -> "application/json"), "{\"oidHenkilo\":\"oid\",\"kutsumanimi\":\"full\",\"sukunimi\":\"name\"}")
    )
  )

  val config = RemoteApplicationConfig(
    casUrl = "http://localhost/cas/v1/tickets",
    username = "foo",
    password = "bar",
    henkilohallintaUrl = "http://localhost/authentication-service/resources/s2s/byHetu/",
    ticketConsumerUrl = "http://localhost/authentication-service/j_spring_cas_security_check"
  )

  test("getHenkiloByHetu should call henkilopalvelu using authentication cookies") {
    val mock = createMock()
    val service = new RemoteAuthenticationInfoService(config, mock)
    service.getHenkiloByHetu("111111-1975") should equal (true, Some(UserInfo("oid", "full name")))
  }

  test("should cache authentication cookies") {
    val mock = createMock()
    val service = new RemoteAuthenticationInfoService(config, mock)
    service.getHenkiloByHetu("111111-1975")
    service.getHenkiloByHetu("111111-1975")
    mock.requests("http://localhost/cas/v1/tickets") should equal (1)
    mock.requests("http://localhost/cas/v1/tickets/TGT-63528-7e6K4Ft6YCbiiLFdjk7Y-cas.foo") should equal(1)
    mock.requests("http://localhost/authentication-service/resources/s2s/byHetu/111111-1975") should equal(2)
  }

  test("getHenkiloByHetu should get a new session if the old one expires") {
    val mock = createMock()
    val service = new RemoteAuthenticationInfoService(config, mock, ttl = Duration(100, TimeUnit.MILLISECONDS))
    service.getHenkiloByHetu("111111-1975")
    Thread.sleep(200)
    service.getHenkiloByHetu("111111-1975")
    mock.requests("http://localhost/cas/v1/tickets") should equal(2)
    mock.requests("http://localhost/cas/v1/tickets/TGT-63528-7e6K4Ft6YCbiiLFdjk7Y-cas.foo") should equal(2)
    mock.requests("http://localhost/authentication-service/resources/s2s/byHetu/111111-1975") should equal(2)
  }

}


class Mocks(mockedRequests: Map[(String, String), HttpRequest]) extends HttpClient {
  var requests: Map[String, Int] = Map()
  private def incCount(url: String) = synchronized {
    requests = requests + (url -> (requests.getOrElse(url, 0) + 1))
  }
  override def httpGet(url: String): HttpRequest = {
    incCount(url)
    mockedRequests(("GET", url))
  }
  override def httpPost(url: String, data: Option[String]): HttpRequest = {
    incCount(url)
    mockedRequests("POST", url)
  }
  override def httpGet(url: String, options: HttpOption*): HttpRequest = {
    incCount(url)
    mockedRequests(("GET", url))
  }
}


case class MockResponse(responseCode: Int, headers: Map[String, String], body: String, resp: Option[String] = None) extends HttpRequest {
  override def responseWithHeaders(): (Int, Map[String, String], String) = (responseCode, headers, body)
  override def header(key: String, value: String): HttpRequest = this
  override def param(key: String, value: String): HttpRequest = this
  override def response(): Option[String] = resp
}
