package fi.vm.sade.attributeauthority

import com.fasterxml.jackson.core.JsonParseException
import fi.vm.sade.attributeauthority.util.Logging
import org.json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scalacache._
import guava._
import memoization._
import scala.concurrent.duration._

case class UserInfo(oid: String, name: String)

object UserInfo {
  implicit val formats: Formats = DefaultFormats

  def fromJson(jsonData: String): Option[UserInfo] = {
    for {
      obj <- parseJson(jsonData)
      oid = (obj \ "oidHenkilo").extract[String]
      fname = (obj \ "kutsumanimi").extract[String]
      sname = (obj \ "sukunimi").extract[String]
    } yield UserInfo(oid, fname + " " + sname)
  }

  def parseJson(jsonData: String): Option[json4s.JValue] = {
    try {
      Some(parse(jsonData))
    }
    catch {
      case _ : JsonParseException => None
    }
  }
}

class MockAuthenticationInfoService extends AuthenticationInfoService {
  def getHenkiloByHetu(hetu: String): (Boolean, Option[UserInfo]) = {
    TestFixture.persons.get(hetu) match {
      case Some(user) => (true, UserInfo.fromJson(user))
      case _ => (true, None)
    }
  }
}

case class CannotAuthenticateException(responseCode: Option[Int], headers: Map[String, List[String]])
  extends Exception(s"cannot authenticate, response code $responseCode, headers: $headers")

class RemoteAuthenticationInfoService(config: RemoteApplicationConfig, client: HttpClient = DefaultHttpClient, ttl: Duration = 30.minutes) extends AuthenticationInfoService with Logging {
  implicit val scalaCache = ScalaCache(GuavaCache())

  private def getCookies(retryCount: Int = 0): List[String] = {
    def retry(count: Int, responseCode: Option[Int], headers: Map[String, List[String]]): List[String] = {
      if (count < 3) {
        logger.warn(s"retrying getCookies, retry attempt #${count + 1}...")
        getCookies(count + 1)
      } else {
        throw CannotAuthenticateException(responseCode, headers)
      }
    }
    def getCookieHeaders = CASClient(client).getServiceTicket(config) match {
      case Some(ticket) =>
        val (responseCode, headersMap, _) = client.httpGet(config.ticketConsumerUrl)
          .header("CasSecurityTicket", ticket)
          .responseWithHeaders()
        (responseCode, headersMap.get("Set-Cookie")) match {
          case (401, _) => retry(retryCount, Some(responseCode), headersMap)
          case (_, Some(cookies)) if cookies.exists(_.startsWith("JSESSIONID")) => cookies.map(_.split(';').head)
          case (_, _) => retry(retryCount, Some(responseCode), headersMap)
        }
      case None => retry(retryCount, None, Map())
    }
    getCookieHeaders
  }

  private def cachedOrNewCookies(getNewCookies: Boolean = false): List[String] = getNewCookies match {
    case true => getCookies()
    case false => memoize(ttl) { getCookies() }
  }

  private def addHeaders(request: HttpRequest, newCookies: Boolean) {
    request.header("Cookie", cachedOrNewCookies(newCookies).mkString("; "))
    request.header("Caller-Id", "attribute-authority.attributeauthority.backend")
  }

  def getHenkiloByHetu(hetu: String): (Boolean, Option[UserInfo]) = {
    def tryGet(hetu: String, newCookies: Boolean = false, retryCount: Int = 0): (Boolean, Option[UserInfo]) = {
      val request: HttpRequest = client.httpGet(config.henkilohallintaUrl + hetu)
      addHeaders(request, newCookies)
      val (responseCode, _, resultString) = request.responseWithHeaders()

      responseCode match {
        case 401 if retryCount < 2 =>
          logger.info("session expired, trying to get a new one")
          tryGet(hetu, newCookies = true, retryCount + 1)
        case 200 => (true, UserInfo.fromJson(resultString))
        case 404 => (true, None)
        case _ => {
          logger.warn("unexpected response code " + responseCode + " from " + config.henkilohallintaUrl)
          (false, None)
        }
      }
    }
    tryGet(hetu)
  }
}

trait AuthenticationInfoService {
  def getHenkiloByHetu(hetu: String): (Boolean, Option[UserInfo])
}
