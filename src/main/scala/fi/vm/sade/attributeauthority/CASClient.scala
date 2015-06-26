package fi.vm.sade.attributeauthority

import fi.vm.sade.attributeauthority.util.Logging

case class CASClient(httpClient: HttpClient) extends Logging {

  protected def getTicketGrantingTicket(service: RemoteApplicationConfig): Option[String] = {
    val (responseCode, headersMap, resultString) = httpClient.httpPost(service.casUrl, None)
      .param("username", service.username)
      .param("password", service.password)
      .responseWithHeaders()

    responseCode match {
      case 201 =>
        val ticketPattern = """.*/([^/]+)""".r
        val headerValue = headersMap.getOrElse("Location",List("no location header")).head
        ticketPattern.findFirstMatchIn(headerValue) match {
          case Some(matched) => Some(matched.group(1))
          case None => {
            logger.warn("no ticket in response")
            None
          }
        }
      case _ =>
        logger.warn("failed getting ticket granting ticket")
        logger.warn(resultString)
        None
    }
  }

  def getServiceTicket(service: RemoteApplicationConfig): Option[String] =
    getTicketGrantingTicket(service).flatMap { ticket =>
      httpClient.httpPost(service.casUrl + "/" + ticket, None)
        .param("service", service.ticketConsumerUrl)
        .response()
    }
}
