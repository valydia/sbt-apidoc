import models.gathr.culpinteam.v1.EventDao
import org.slf4j.LoggerFactory
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import service.{ FacebookManager, EmailManager }
import scala.concurrent.ExecutionContext.Implicits.global

import views.html.gathr.culpinteam.v1.{ welcome => welcomePage }

import scala.concurrent.Future


object Application extends Controller {

  val logger = LoggerFactory.getLogger(getClass)

  val emailForm = Form(
    mapping(
      "body" -> nonEmptyText,
      "email" -> email,
      "firstname" -> nonEmptyText
    )(Email.apply)(Email.unapply)
  )

  def welcome(): Action[AnyContent] = ???

  def sendEmail(): Action[AnyContent] = ???

  def sendHomeEmail(email: Email): Future[Boolean] = ???

  def testFacebookEvent(): Action[AnyContent] = TODO

}