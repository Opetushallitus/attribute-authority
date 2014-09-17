package fi.vm.sade.attributeauthority

import org.scalatra._

trait AttributeAuthorityStack extends ScalatraServlet /*with ScalateSupport*/ {
  notFound {
    // remove content type in case it was set through an action
    contentType = null
  }
}
