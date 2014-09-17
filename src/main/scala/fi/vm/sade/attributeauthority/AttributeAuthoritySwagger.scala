package fi.vm.sade.attributeauthority

import org.scalatra.swagger.{ApiInfo, Swagger}

class AttributeAuthoritySwagger extends Swagger(
  Swagger.SpecVersion, BuildInfo.version,
  ApiInfo("attribute-authority",
    "Attribute Authority service",
    "https://opintopolku.fi/wp/fi/opintopolku/tietoa-palvelusta/",
    "verkkotoimitus_opintopolku@oph.fi",
    "EUPL 1.1 or latest approved by the European Commission",
    "http://www.osor.eu/eupl/"))
