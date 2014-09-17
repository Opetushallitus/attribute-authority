package fi.vm.sade.attributeauthority

import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
 *
 */

case class UserInfo(private val jsonData: String) {
  val (oid, name) = (for {
    JObject(child) <- parse(jsonData)
    JField("oidHenkilo", JString(oid)) <- child
    JField("kutsumanimi", JString(fname)) <- child
    JField("sukunimi", JString(sname)) <- child
  } yield List(oid, fname, sname)) match {
    case List(l) => {
      l match {
        case List(oid, fname, sname) => (oid, fname + " " + sname)
        case _ => (None, None)
      }
    }
    case _ => (None, None)
  }
}

class MockAuthenticationInfoService extends AuthenticationInfoService {
  def getHenkiloByHetu(hetu: String): Option[UserInfo] = {
    TestFixture.persons.get(hetu) match {
      case Some(user) => Some(UserInfo(user))
      case _ => None
    }
  }
}

class RemoteAuthenticationInfoService(config: RemoteApplicationConfig) extends AuthenticationInfoService {

  def getHenkiloByHetu(hetu : String) : Option[UserInfo] = {
    CASClient(DefaultHttpClient).getServiceTicket(config) match {
      case None => {
        println("failed getting service ticket")
        None
      }
      case Some(ticket) => getHenkilo(hetu, ticket)
    }
  }

  private def getHenkilo(hetu: String, serviceTicket: String): Option[UserInfo] = {
    val (responseCode, headersMap, resultString) = DefaultHttpClient.httpGet(config.henkilohallintaUrl + "/" + hetu)
      .param("ticket", serviceTicket)
      .responseWithHeaders

    responseCode match {
      case 404 => None
      case _ => Some(UserInfo(resultString))
    }
  }
  }

trait AuthenticationInfoService {
  def getHenkiloByHetu(hetu : String) : Option[UserInfo]
}
