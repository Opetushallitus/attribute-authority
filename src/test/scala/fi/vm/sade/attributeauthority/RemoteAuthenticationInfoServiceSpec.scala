package fi.vm.sade.attributeauthority

import java.util.concurrent.TimeUnit

import org.scalatra.test.specs2.ScalatraSpec
import org.specs2.specification.Fragments

import scala.concurrent.duration.Duration

class RemoteAuthenticationInfoServiceSpec extends ScalatraSpec {
  override def is: Fragments = p ^
    "RemoteAuthenticationInfoService" ^ br ^
    t ^ "getHenkiloByHetu should call henkilopalvelu using authentication cookies" ! getHenkiloByHetu ^ br ^
    "should cache authentication cookies" ! cachedCookies ^ br ^
    "getHenkiloByHetu should get a new session if the old one expires" ! newSession ^ br ^
    end

  def createMock() = new Mocks(
    Map(
      ("POST", "http://localhost/cas/v1/tickets") ->
        MockResponse(201, Map("Location" -> List("http://localhost/cas/v1/tickets/TGT-63528-7e6K4Ft6YCbiiLFdjk7Y-cas.foo")), ""),

      ("POST", "http://localhost/cas/v1/tickets/TGT-63528-7e6K4Ft6YCbiiLFdjk7Y-cas.foo") ->
        MockResponse(200, Map(), "", Some("ST-63529-TQpkXpIE0YgGXFIwTHaj-cas.foo")),

      ("GET", "http://localhost/authentication-service/j_spring_cas_security_check") ->
        MockResponse(200, Map("Set-Cookie" -> List("JSESSIONID=9C16A50F8E5DE52D03F237CB3500D3A8")), ""),

      ("GET", "http://localhost/authentication-service/resources/s2s/byHetu/111111-1975") ->
        MockResponse(200, Map("Content-Type" -> List("application/json")), "{\"oidHenkilo\":\"oid\",\"kutsumanimi\":\"full\",\"sukunimi\":\"name\"}")
    )
  )

  val config = RemoteApplicationConfig(
    casUrl = "http://localhost/cas/v1/tickets",
    username = "foo",
    password = "bar",
    henkilohallintaUrl = "http://localhost/authentication-service/resources/s2s/byHetu/",
    ticketConsumerUrl = "http://localhost/authentication-service/j_spring_cas_security_check"
  )


  def getHenkiloByHetu = {
    val mock = createMock()
    val service = new RemoteAuthenticationInfoService(config, mock)
    service.getHenkiloByHetu("111111-1975") must_== (true, Some(UserInfo("oid", "full name")))
  }

  def cachedCookies = {
    val mock = createMock()
    val service = new RemoteAuthenticationInfoService(config, mock)
    service.getHenkiloByHetu("111111-1975")
    service.getHenkiloByHetu("111111-1975")
    mock.requests("http://localhost/cas/v1/tickets") must_== 1
    mock.requests("http://localhost/cas/v1/tickets/TGT-63528-7e6K4Ft6YCbiiLFdjk7Y-cas.foo") must_== 1
    mock.requests("http://localhost/authentication-service/resources/s2s/byHetu/111111-1975") must_== 2
  }

  def newSession = {
    val mock = createMock()
    val service = new RemoteAuthenticationInfoService(config, mock, ttl = Duration(100, TimeUnit.MILLISECONDS))
    service.getHenkiloByHetu("111111-1975")
    Thread.sleep(200)
    service.getHenkiloByHetu("111111-1975")
    mock.requests("http://localhost/cas/v1/tickets") must_== 2
    mock.requests("http://localhost/cas/v1/tickets/TGT-63528-7e6K4Ft6YCbiiLFdjk7Y-cas.foo") must_== 2
    mock.requests("http://localhost/authentication-service/resources/s2s/byHetu/111111-1975") must_== 2
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
}


case class MockResponse(responseCode: Int, headers: Map[String, List[String]], body: String, resp: Option[String] = None) extends HttpRequest {
  override def responseWithHeaders(): (Int, Map[String, List[String]], String) = (responseCode, headers, body)
  override def header(key: String, value: String): HttpRequest = this
  override def param(key: String, value: String): HttpRequest = this
  override def response(): Option[String] = resp
}
