package fi.vm.sade.attributeauthority

import com.fasterxml.jackson.core.JsonParseException
import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
 *
 */

case class UserInfo(val oid: String, val name: String)

object UserInfo {
  def fromJson(jsonData: String): Option[UserInfo] = {
    try {
      (for {
        JObject(child) <- parse(jsonData)
        JField("oidHenkilo", JString(oid)) <- child
        JField("kutsumanimi", JString(fname)) <- child
        JField("sukunimi", JString(sname)) <- child
      } yield List(oid, fname, sname)) match {
        case List(l) => {
          l match {
            case List(oid, fname, sname) => Some(UserInfo(oid, fname + " " + sname))
            case _ => None
          }
        }
        case _ => None
      }
    } catch {
      case _: JsonParseException => None
    }
  }
}

class MockAuthenticationInfoService extends AuthenticationInfoService {
  def getHenkiloByHetu(hetu: String): Option[UserInfo] = {
    TestFixture.persons.get(hetu) match {
      case Some(user) => UserInfo.fromJson(user)
      case _ => None
    }
  }
}

class RemoteAuthenticationInfoService(config: RemoteApplicationConfig) extends AuthenticationInfoService {

  def getHenkiloByHetu(hetu : String) : Option[UserInfo] = {
    CASClient(DefaultHttpClient).getServiceTicket(config) match {
      case None => None
      case Some(ticket) => getHenkilo(hetu, ticket)
    }
  }

  private def getHenkilo(hetu: String, serviceTicket: String): Option[UserInfo] = {
    val (responseCode, headersMap, resultString) = DefaultHttpClient.httpGet(config.henkilohallintaUrl + "/" + hetu)
      .param("ticket", serviceTicket)
      .responseWithHeaders

    responseCode match {
      case 200 => UserInfo.fromJson(resultString)
      case _ => None
    }
  }
}

trait AuthenticationInfoService {
  def getHenkiloByHetu(hetu : String) : Option[UserInfo]
}
