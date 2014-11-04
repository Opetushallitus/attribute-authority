package fi.vm.sade.attributeauthority

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{Swagger, _}

import scala.xml.{SAXParseException, Elem, XML}

class AttributeAuthorityServlet(implicit val appConfig: AppConfig, implicit val swagger: Swagger) extends ScalatraServlet with SwaggerSupport {

  protected val applicationDescription = "API jolla mapataan HETU OID:ksi"

  before() {
    contentType = "text/xml"
  }

  val postOidSwagger = (apiOperation[String]("postOid")
    summary "Muutetaan HETU OIDksi"
    parameter bodyParam[String]("").description("SAML2 Attribute Query"))

  private def getHetu(msg: Elem): Option[String] = {
    (for {
      item <- msg \\ "NameID" if (item \ "@Format").text == "urn:oid:1.2.246.21"
    } yield item.text) match {
      case List(hetu) if !hetu.isEmpty => Some(hetu.trim)
      case _ => None
    }
  }

  private def getMsgId(msg: Elem): String = {
    (for {
      idAttr <- msg \\ "AttributeQuery" \ "@ID"
    } yield idAttr.text) match {
      case List(id) => id.trim
      case _ => ""
    }
  }

  private def newUUID = java.util.UUID.randomUUID.toString

  private def getISODate(secondsToAdd: Int = 0) = {
    val fmt = ISODateTimeFormat.dateTimeNoMillis.withZoneUTC
    DateTime.now.withDurationAdded(secondsToAdd, 1000).toString(fmt)
  }

  private def samlResponse(user: UserInfo, rid: String) = {
    try {
      val uuid1 = newUUID
      val uuid2 = newUUID
      val currentTime = getISODate()
      val futureTime = getISODate(appConfig.saml2ResponseValidityTimeSeconds)
      Some(
        <soap11:Envelope xmlns:soap11="http://schemas.xmlsoap.org/soap/envelope/">
          <soap11:Body>
            <saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
                             ID={ uuid1 }
                             InResponseTo={ rid }
                             IssueInstant={ currentTime }
                             Version="2.0">
              <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" Format="urn:oasis:names:tc:SAML:2.0:nameid-format:entity">{ appConfig.saml2IssuerUrl }</saml2:Issuer>
              <saml2p:Status>
                <saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
              </saml2p:Status>
              <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" ID={ uuid2 } IssueInstant={ currentTime } Version="2.0">
                <saml2:Issuer Format="urn:oasis:names:tc:SAML:2.0:nameid-format:entity">{ appConfig.saml2IssuerUrl }</saml2:Issuer>
                <saml2:Subject>
                  <saml2:NameID>{ appConfig.saml2NameID }</saml2:NameID>
                  <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:sender-vouches">
                    <saml2:SubjectConfirmationData Address="178.217.129.16" InResponseTo={ rid } NotOnOrAfter={ futureTime }/>
                  </saml2:SubjectConfirmation>
                </saml2:Subject>
                <saml2:AttributeStatement>
                  <saml2:Attribute FriendlyName="oid" Name="urn:oid:2.16.840.1.113730.3.1.241" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri">
                    <saml2:AttributeValue xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">{ user.name }</saml2:AttributeValue>
                  </saml2:Attribute>
                  <saml2:Attribute FriendlyName="oid" Name="urn:oid:2.5.4.10" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri">
                    <saml2:AttributeValue xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">{ user.oid }</saml2:AttributeValue>
                  </saml2:Attribute>
                </saml2:AttributeStatement>
              </saml2:Assertion>
            </saml2p:Response>
          </soap11:Body>
        </soap11:Envelope>
      )
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
        None
      }
    }
  }

  private def samlErrorResponse(statusCode: String, statusMessage: String) = {
    val uuid = newUUID
    val currentTime = getISODate()
    <soap11:Envelope xmlns:soap11="http://schemas.xmlsoap.org/soap/envelope/">
      <soap11:Body>
        <saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol" ID={ uuid } IssueInstant={ currentTime } Version="2.0">
          <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" Format="urn:oasis:names:tc:SAML:2.0:nameid-format:entity">{ appConfig.saml2IssuerUrl }</saml2:Issuer>
          <saml2p:Status>
            <saml2p:StatusCode Value={ statusCode }/>
            <saml2p:StatusMessage>{ statusMessage }</saml2p:StatusMessage>
          </saml2p:Status>
        </saml2p:Response>
      </soap11:Body>
    </soap11:Envelope>
  }

  private def soapFaultMessage(faultCode: String, faultString: String) = {
    <SOAP-ENV:Envelope
        xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"
        xmlns:xsi="http://www.w3.org/1999/XMLSchema-instance"
        xmlns:xsd="http://www.w3.org/1999/XMLSchema">
      <SOAP-ENV:Body>
        <SOAP-ENV:Fault>
          <faultcode>{ faultCode }</faultcode>
          <faultstring>{ faultString }</faultstring>
        </SOAP-ENV:Fault>
      </SOAP-ENV:Body>
    </SOAP-ENV:Envelope>
  }

  post("/hetuToOid", operation(postOidSwagger)) {
    try {
      val msg = XML.loadString(request.body)
      getHetu(msg) match {
        case Some(hetu) => {
          appConfig.authenticationInfoService.getHenkiloByHetu(hetu) match {
            case Some(user) => {
              samlResponse(user, getMsgId(msg)) match {
                case Some(reply) => reply
                case _ => halt(status = 500, body = soapFaultMessage("soap11:Server", "Internal error"))
              }
            }
            case _ => samlErrorResponse("urn:oasis:names:tc:SAML:2.0:status:Responder", "No user found by hetu")
          }
        }
        case _ => samlErrorResponse("urn:oasis:names:tc:SAML:2.0:status:Requester", "No hetu found in request")
      }
    }
    catch {
      case e: Exception => halt(status = 500, body = soapFaultMessage("soap11:Client", "Invalid message format"))
    }
  }

  get("/") {
    contentType = "text/html"
    <html>
      <body>
        <h1>Attribute Authority Service</h1>
      </body>
    </html>
  }

  get("/buildversion.txt") {
    contentType = "text/plain"
    s"""name=${BuildInfo.name}
       |version=${BuildInfo.version}
     """.stripMargin
  }
}
