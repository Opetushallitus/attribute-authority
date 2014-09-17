package fi.vm.sade.attributeauthority

/**
 *
 */
object TestFixture {
  val authServiceJsonResponse1 = "{\"oidHenkilo\": \"1.2.246.562.24.14229104472\", \"kutsumanimi\": \"Teppo\", \"sukunimi\": \"Testaaja\"}"
  val authServiceJsonResponse2 = "{\"oidHenkilo\": \"1.2.246.562.24.99178889818\", \"kutsumanimi\": \"Perus\", \"sukunimi\": \"Pingviini\"}"
  val persons = Map(
    ("010101-123N", authServiceJsonResponse1),
    ("010969-929N", authServiceJsonResponse2)
  )
}
