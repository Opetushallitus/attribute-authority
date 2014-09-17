package fi.vm.sade.attributeauthority

import java.io.File

import com.typesafe.config.{Config, ConfigException, ConfigFactory}


/**
 *
 */

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
      conf.getString(key) match {
        case s: String if !s.isEmpty => s
        case _ => default
      }
    }

    private def intFromProps(key: String, default: Int): Int = {
      conf.getInt(key) match {
        case i: Int if i != 0 => i
        case _ => default
      }
    }

    override val saml2IssuerUrl: String = stringFromProps("saml2_issuer_url", saml2IssuerUrl)
    override val saml2NameID: String = stringFromProps("saml2_name_id", saml2NameID)
    override val saml2ResponseValidityTimeSeconds: Int = intFromProps("saml2_response_validity_time_seconds", saml2ResponseValidityTimeSeconds)
    
    override val henkilohallintaUrl: String = stringFromProps("henkilohallinta_url", henkilohallintaUrl)
    override val henkilohallintaUsername: String = stringFromProps("henkilohallinta_username", henkilohallintaUsername)
    override val henkilohallintaPassword: String = stringFromProps("henkilohallinta_password", henkilohallintaPassword)

    override val casTicketUrl: String = stringFromProps("cas_ticket_url", casTicketUrl)
    override val ticketConsumerUrl: String = stringFromProps("cas_ticket_consumer_url", casTicketUrl)

    override val authenticationInfoService: AuthenticationInfoService = casTicketUrl match {
      case url if !url.isEmpty => new RemoteAuthenticationInfoService(RemoteApplicationConfig(
        url, henkilohallintaUsername, henkilohallintaPassword, henkilohallintaUrl, ticketConsumerUrl))
      case _ => authenticationInfoService
    }


  }
}

trait AppConfig {
  val saml2IssuerUrl: String = "https://localhost/oidresolver"
  val saml2NameID: String = "TestID"
  val saml2ResponseValidityTimeSeconds: Int = 3600
  val henkilohallintaUrl: String = ""
  val henkilohallintaUsername: String = ""
  val henkilohallintaPassword: String = ""
  val casTicketUrl: String = ""
  val ticketConsumerUrl: String = ""
  val authenticationInfoService: AuthenticationInfoService = new MockAuthenticationInfoService
}

case class RemoteApplicationConfig(casUrl: String, username: String, password: String, henkilohallintaUrl: String, ticketConsumerUrl: String)