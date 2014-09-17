package fi.vm.sade.attributeauthority

import org.scalatra.test.specs2.ScalatraSpec


trait ScalatraTestSupport extends ScalatraSpec {
  implicit val config: AppConfig = AppConfigSetup.create
  implicit val swagger = new AttributeAuthoritySwagger
}

object AppConfigSetup {
  lazy val create = AppConfig.fromSystemProperty
}