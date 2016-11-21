package fi.vm.sade.attributeauthority

import org.scalatra.test.scalatest.ScalatraFunSuite


trait ScalatraTestSupport extends ScalatraFunSuite {
  implicit val config: AppConfig = AppConfigSetup.create
  implicit val swagger = new AttributeAuthoritySwagger
}

object AppConfigSetup {
  lazy val create = AppConfig.fromSystemProperty
}