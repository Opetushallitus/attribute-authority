import fi.vm.sade.attributeauthority._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {
  implicit val config = AppConfig.fromSystemProperty
  implicit val swagger = new AttributeAuthoritySwagger
  override def init(context: ServletContext) {
    context.mount(new SwaggerServlet, "/swagger/*")
    context.mount(new AttributeAuthorityServlet, "/*")
  }
}
