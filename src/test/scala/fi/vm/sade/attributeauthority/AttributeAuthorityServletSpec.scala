package fi.vm.sade.attributeauthority

import org.scalatra.test.specs2._
import scala.xml.{Elem, XML}

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html
class AttributeAuthorityServletSpec extends ScalatraTestSupport { def is =
  "GET / on AttributeAuthorityServlet"                         ^ br ^
  t ^ "should return status 200"                               ! root200 ^ br ^
  bt ^ bt ^ "POST / on AttributeAuthorityServlet"                    ^ br ^
  t ^ "should return proper SAML message"                      ! hetu ^ br ^
  "should return proper SAML message for other hetu"       ! hetu2 ^ br ^
  end

  addServlet(new AttributeAuthorityServlet, "/*")

  def root200 = get("/") {
    status must_== 200
  }

  def postBody(hetu: String) = <samlp:AttributeQuery
    xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
    xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
    ID="aaf23196-1773-2113-474a-fe114412ab72"
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
    </samlp:AttributeQuery>

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

  def hetu = post("/", postBody("010969-929N").toString.getBytes) {
    val msg: Elem = XML.loadString(response.body)
    (status, getOid(msg), getName(msg)) must_== (200, "1.2.246.562.24.99178889818", "Perus Pingviini")

  }

  def hetu2 = post("/", postBody("010101-123N").toString.getBytes) {
    val msg: Elem = XML.loadString(response.body)
    (200, getOid(msg), getName(msg)) must_== (200, "1.2.246.562.24.14229104472", "Teppo Testaaja")
  }
}
