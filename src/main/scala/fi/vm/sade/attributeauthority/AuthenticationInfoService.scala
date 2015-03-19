package fi.vm.sade.attributeauthority

import com.fasterxml.jackson.core.JsonParseException
import fi.vm.sade.attributeauthority.util.Logging
import org.json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

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

class RemoteAuthenticationInfoService(config: RemoteApplicationConfig) extends AuthenticationInfoService with Logging {

  def getHenkiloByHetu(hetu: String): (Boolean, Option[UserInfo]) = {
    CASClient(DefaultHttpClient).getServiceTicket(config) match {
      case None => (false, None)
      case Some(ticket) => getHenkilo(hetu, ticket)
    }
  }

  private def getHenkilo(hetu: String, serviceTicket: String): (Boolean, Option[UserInfo]) = {
    val (responseCode, headersMap, resultString) = DefaultHttpClient.httpGet(config.henkilohallintaUrl + hetu)
      .param("ticket", serviceTicket)
      .responseWithHeaders

    responseCode match {
      case 200 => (true, UserInfo.fromJson(resultString))
      case 404 => (true, None)
      case _ => {
        logger.warn("unexpected response code " + responseCode + " from " + config.henkilohallintaUrl)
        (false, None)
      }
    }
  }
}

trait AuthenticationInfoService {
  def getHenkiloByHetu(hetu: String): (Boolean, Option[UserInfo])
}
