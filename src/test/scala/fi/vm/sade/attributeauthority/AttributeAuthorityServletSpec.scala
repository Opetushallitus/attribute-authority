package fi.vm.sade.attributeauthority

import org.joda.time.format.ISODateTimeFormat

import scala.xml.{Elem, XML}

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html
class AttributeAuthorityServletSpec extends ScalatraTestSupport { def is = br ^
  "GET / on AttributeAuthorityServlet"                                  ^ br ^
  t ^ "should return status 200"                                        ! root200 ^ br ^
  bt ^ bt ^ "POST /hetuToOid on AttributeAuthorityServlet"              ^ br ^
  t ^ "should return proper SAML message"                               ! hetu ^ br ^
  "should return proper SAML message for other hetu"                    ! hetu2 ^ br ^
  "should return proper SAML message for misssing hetu"                 ! nonExistingHetu ^ br ^
  "should return proper SAML message if no hetu in request"             ! hetuMissingFromRequest ^ br ^
  "should return proper SAML message if no id in request"               ! idMissingFromRequest ^ br ^
  "should return proper SAML message if neither heto nor id in request" ! idMissingFromRequest ^ br ^
  "should return proper SAML message for empty request"                 ! emptyRequest ^ br ^
  "SAML message has proper Issuer URL"                                  ! properIssuerUrl ^ br ^
  "SAML message has proper IssueInstant"                                ! properIssueInstant ^ br ^
  "SAML message has proper NotOnOrAfter"                                ! properNotOnOrAfter ^ br ^
  bt ^ bt ^ "GET /buildversion.txt on AttributeAuthorityServlet"        ^ br ^
  t ^ "should return proper version"                                    ! version ^ br ^
  bt ^ bt ^ "UserInfo"                                                  ^ br ^
  t ^ "parses JSON correctly"                                           ! userInfo ^ br ^
  "deals with bad input"                                                ! userInfoBadInput ^ br ^
  end

  addServlet(new AttributeAuthorityServlet, "/*")

  def root200 = get("/") {
    status must_== 200
  }

  def postBody(msgId: String, hetu: String) = <samlp:AttributeQuery
    xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
    xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
    ID={ msgId }
    Version="2.0"
    IssueInstant="2006-07-17T20:31:40">
      <saml:Issuer Format="urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName">
        CN=trscavo@uiuc.edu,OU=User,O=NCSA-TEST,C=US
      </saml:Issuer>
      <saml:Subject>
        <saml:NameID Format="urn:oid:1.2.246.21">
          { hetu }
        </saml:NameID>
      </saml:Subject>
      <saml:Attribute NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"
                      Name="urn:oid:2.5.4.42"
                      FriendlyName="givenName">
      </saml:Attribute>
      <saml:Attribute
      NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri"
      Name="urn:oid:1.3.6.1.4.1.1466.115.121.1.26"
      FriendlyName="mail">
      </saml:Attribute>
    </samlp:AttributeQuery>.toString.getBytes

  private def getAttrValue(msg: Elem, name: String) = {
    (for {
      tg <- msg \\ "Attribute" if (tg \ "@Name").text == name
      item <- tg \ "AttributeValue"
    } yield item.text) match {
      case List(item) => item.trim
      case _ => ""
    }
  }

  private def getName(msg: Elem) = getAttrValue(msg, "urn:oid:2.16.840.1.113730.3.1.241")
  private def getOid(msg: Elem) = getAttrValue(msg, "urn:oid:2.5.4.10")

  def userInfo = {
    (TestFixture.persons.get("010101-123N") match {
      case Some(info) => {
        UserInfo.fromJson(info) match {
          case Some(u) => (u.oid, u.name)
          case _ =>
        }
      }
      case _ =>
    }) must_== ("1.2.246.562.24.14229104472", "Teppo Testaaja")
  }

  def userInfoBadInput = {
    val bad = UserInfo.fromJson("bad input")
    bad must_== None
  }

  def hetu = post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "010969-929N")) {
    val msg: Elem = XML.loadString(response.body)
    (status, getOid(msg), getName(msg)) must_== (200, "1.2.246.562.24.99178889818", "Perus Pingviini")

  }

  def hetu2 = post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "010101-123N")) {
    val msg: Elem = XML.loadString(response.body)
    (status, getOid(msg), getName(msg)) must_== (200, "1.2.246.562.24.14229104472", "Teppo Testaaja")
  }

  def nonExistingHetu = post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "111111-123N")) {
    val msg: Elem = XML.loadString(response.body)
    (msg \\ "Status" \ "StatusCode" \ "@Value").text must_== "urn:oasis:names:tc:SAML:2.0:status:Responder"
  }

  def hetuMissingFromRequest = post("/hetuToOid", postBody("", "111111-123N")) {
    val msg: Elem = XML.loadString(response.body)
    (msg \\ "Status" \ "StatusCode" \ "@Value").text must_== "urn:oasis:names:tc:SAML:2.0:status:Requester"
  }

  def idMissingFromRequest = post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "")) {
    val msg: Elem = XML.loadString(response.body)
    (msg \\ "Status" \ "StatusCode" \ "@Value").text must_== "urn:oasis:names:tc:SAML:2.0:status:Requester"
  }

  def hetuAndIdMissingFromRequest = post("/hetuToOid", postBody("", "")) {
    val msg: Elem = XML.loadString(response.body)
    (msg \\ "Status" \ "StatusCode" \ "@Value").text must_== "urn:oasis:names:tc:SAML:2.0:status:Requester"
  }

  def emptyRequest = post("/hetuToOid", "".getBytes) {
    val msg: Elem = XML.loadString(response.body)
    (status, (msg \\ "Fault" \ "faultcode").text) must_== (500, "soap11:Client")
  }

  def properIssuerUrl = post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "111111-123N")) {
    val msg: Elem = XML.loadString(response.body)
    (msg \\ "Issuer").text must_== config.saml2IssuerUrl
  }

  def properIssueInstant = post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "010101-123N")) {
    val msg: Elem = XML.loadString(response.body)
    val parser = ISODateTimeFormat.dateTimeNoMillis.withZoneUTC
    parser.parseDateTime((msg \\ "Response" \ "@IssueInstant").text.trim).isAfterNow must_!= true
  }

  def properNotOnOrAfter = post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "010101-123N")) {
    val msg: Elem = XML.loadString(response.body)
    val parser = ISODateTimeFormat.dateTimeNoMillis.withZoneUTC
    parser.parseDateTime((msg \\ "SubjectConfirmationData" \ "@NotOnOrAfter").text.trim).isAfterNow must_== true
  }

  def getVersion(buildinfo: String): String = {
    (for {
      x <- buildinfo.split("\n") if x.startsWith("version=")
    } yield x.split("=")(1)) match {
      case Array(x) => x
      case _ => ""
    }
  }

  def version = get("/buildversion.txt") {
    getVersion(response.body.trim) must_== BuildInfo.version
  }
}
