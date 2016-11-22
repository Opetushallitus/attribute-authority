package fi.vm.sade.attributeauthority

import java.io.File
import java.nio.file.Paths

import com.typesafe.config.{ConfigException, ConfigFactory}
import fi.vm.sade.scalaproperties.OphProperties


/**
 *
 */

object UrlProperties extends OphProperties("/attributeauthority-oph.properties") {
  addOptionalFiles(Paths.get(sys.props.getOrElse("user.home", ""), "/oph-configuration/common.properties").toString)
}

object AppConfig {
  def fromSystemProperty = {
    val file = new File(System.getProperty("user.home") + "/oph-configuration/attribute-authority.properties")
    file.canRead match {
      case true => new FileAppConfig(file)
      case false => new FallbackAppConfig
    }
  }

  class FallbackAppConfig extends AppConfig

  class FileAppConfig(file: File) extends AppConfig {
    val conf = ConfigFactory.load(ConfigFactory.parseFile(file))

    private def stringFromProps(key: String, default: String): String = {
      try {
        conf.getString(key) match {
          case null => default
          case s => s
        }
      }
      catch {
        case _: ConfigException => default
      }
    }

    private def intFromProps(key: String, default: Int): Int = {
      try {
        conf.getInt(key) match {
          case i: Int if i != 0 => i
          case _ => default
        }
      }
      catch {
        case _: ConfigException => default
      }
    }

    override def saml2IssuerUrl: String = UrlProperties.url("attributeauthority.saml2_issuer_url")
    override def saml2NameID: String = stringFromProps("attributeauthority.saml2_name_id", super.saml2NameID)
    override def saml2ResponseValidityTimeSeconds: Int = intFromProps("attributeauthority.saml2_response_validity_time_seconds", super.saml2ResponseValidityTimeSeconds)
    
    override def henkilohallintaUsername: String = stringFromProps("attributeauthority.henkilohallinta_username", super.henkilohallintaUsername)
    override def henkilohallintaPassword: String = stringFromProps("attributeauthority.henkilohallinta_password", super.henkilohallintaPassword)

    override def casTicketUrl: String = stringFromProps("attributeauthority.cas_ticket_url", super.casTicketUrl)
    override def ticketConsumerUrl: String = UrlProperties.url("authentication-service.cas.ticket_consumer_url")

    val authService = casTicketUrl match {
      case url if !url.isEmpty => new RemoteAuthenticationInfoService(RemoteApplicationConfig(
        url, henkilohallintaUsername, henkilohallintaPassword, ticketConsumerUrl))
      case _ => super.authenticationInfoService
    }

    override def authenticationInfoService: AuthenticationInfoService = authService
  }
}

trait AppConfig {
  def saml2IssuerUrl: String = "https://localhost/oidresolver"
  def saml2NameID: String = "TestID"
  def saml2ResponseValidityTimeSeconds: Int = 3600
  def henkilohallintaUsername: String = ""
  def henkilohallintaPassword: String = ""
  def casTicketUrl: String = ""
  def ticketConsumerUrl: String = ""
  def authenticationInfoService: AuthenticationInfoService = new MockAuthenticationInfoService
}

case class RemoteApplicationConfig(casUrl: String, username: String, password: String, ticketConsumerUrl: String)