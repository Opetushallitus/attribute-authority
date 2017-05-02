package fi.vm.sade.attributeauthority

import org.joda.time.format.ISODateTimeFormat

import scala.xml.{Elem, XML}

class AttributeAuthorityServletSpec extends ScalatraTestSupport {

  addServlet(new AttributeAuthorityServlet, "/*")

  test("GET / on AttributeAuthorityServlet should return status 200" ) {
    get("/") {
      status should equal (200)
    }
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

  test("UserInfo parses JSON correctly") {
    (TestFixture.persons.get("010101-123N") match {
      case Some(info) => {
        UserInfo.fromJson(info) match {
          case Some(u) => (u.oid, u.name)
          case _ =>
        }
      }
      case _ =>
    }) should equal ("1.2.246.562.24.14229104472", "Teppo Testaaja")
  }

  test("UserInfo deals with bad input") {
    val bad = UserInfo.fromJson("bad input")
    bad should equal (None)
  }

  test("POST /hetuToOid should return proper SAML message") {
    post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "010969-929N")) {
      val msg: Elem = XML.loadString(response.body)
      (status, getOid(msg), getName(msg)) should equal (200, "1.2.246.562.24.99178889818", "Perus Pingviini")
    }
  }

  test("POST /hetuToOid should return proper SAML message for other hetu") {
    post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "010101-123N")) {
      val msg: Elem = XML.loadString(response.body)
      (status, getOid(msg), getName(msg)) should equal (200, "1.2.246.562.24.14229104472", "Teppo Testaaja")
    }
  }

  test("POST /hetuToOid should return proper SAML message for misssing hetu") {
    post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "111111-123N")) {
      val msg: Elem = XML.loadString(response.body)
      (msg \\ "Assertion" \ "AttributeStatement" \ "Attribute" \ "AttributeValue").text should equal ("https://virkailija.opintopolku.fi/oppijanumerorekisteri-service/henkilo/NOT_FOUND")
    }
  }

  test("POST /hetuToOid should return proper SAML message if no id in request") {
    post("/hetuToOid", postBody("", "111111-123N")) {
      val msg: Elem = XML.loadString(response.body)
      (msg \\ "Status" \ "StatusCode" \ "@Value").text should equal ("urn:oasis:names:tc:SAML:2.0:status:Requester")
    }
  }

  test("POST /hetuToOid should return proper SAML message if no hetu in request")  {
    post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "")) {
      val msg: Elem = XML.loadString(response.body)
      (msg \\ "Status" \ "StatusCode" \ "@Value").text should equal ("urn:oasis:names:tc:SAML:2.0:status:Requester")
    }
  }

  test("POST /hetuToOid should return proper SAML message if neither hetu nor id in request") {
    post("/hetuToOid", postBody("", "")) {
      val msg: Elem = XML.loadString(response.body)
      (msg \\ "Status" \ "StatusCode" \ "@Value").text should equal ("urn:oasis:names:tc:SAML:2.0:status:Requester")
    }
  }

  test("POST /hetuToOid should return proper SAML message for empty request") {
    post("/hetuToOid", "".getBytes) {
      val msg: Elem = XML.loadString(response.body)
      (status, (msg \\ "Fault" \ "faultcode").text) should equal (500, "soap11:Client")
    }
  }

  test("POST /hetuToOid SAML message has proper Issuer URL") {
    post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "111111-123N")) {
      val msg: Elem = XML.loadString(response.body)
      (msg \\ "Response" \ "Issuer").text should equal (config.saml2IssuerUrl)
    }
  }

  test("POST /hetuToOid SAML message has proper IssueInstant") {
    post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "010101-123N")) {
      val msg: Elem = XML.loadString(response.body)
      val parser = ISODateTimeFormat.dateTimeNoMillis.withZoneUTC
      parser.parseDateTime((msg \\ "Response" \ "@IssueInstant").text.trim).isAfterNow should equal(false)
    }
  }

  test("POST /hetuToOid SAML message has proper NotOnOrAfter") {
    post("/hetuToOid", postBody("aaf23196-1773-2113-474a-fe114412ab72", "010101-123N")) {
      val msg: Elem = XML.loadString(response.body)
      val parser = ISODateTimeFormat.dateTimeNoMillis.withZoneUTC
      parser.parseDateTime((msg \\ "SubjectConfirmationData" \ "@NotOnOrAfter").text.trim).isAfterNow should equal (true)
    }
  }

  def getVersion(buildinfo: String): String = {
    (for {
      x <- buildinfo.split("\n") if x.startsWith("version=")
    } yield x.split("=")(1)) match {
      case Array(x) => x
      case _ => ""
    }
  }

  test("GET /buildversion.txt on AttributeAuthorityServlet should return proper version") {
    get("/buildversion.txt") {
      getVersion(response.body.trim) should equal (BuildInfo.version)
    }
  }
}
