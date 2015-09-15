import models.gathr.culpinteam.v1.EventDao
import org.slf4j.LoggerFactory
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import service.{ FacebookManager, EmailManager }
import scala.concurrent.ExecutionContext.Implicits.global

import views.html.gathr.culpinteam.v1.{ welcome => welcomePage }

import scala.concurrent.Future

case class Email(body: String, email: String, firstname: String)

object Application extends Controller {

  val logger = LoggerFactory.getLogger(getClass)

  val emailForm = Form(
    mapping(
      "body" -> nonEmptyText,
      "email" -> email,
      "firstname" -> nonEmptyText
    )(Email.apply)(Email.unapply)
  )

  /**
   * @api {get} / Home page.
   * @apiName Welcome Page.
   * @apiGroup Application
   * @apiVersion 1.0.0
   * @apiDescription Renders the welcome page
   *
   * @apiSuccessExample Success-Response:
   *     HTTP/1.1 200 OK
   *     HTML for welcome page
   *     {
   *       "emailAvailable": "true"
   *     }
   *
   */
  def welcome(): Action[AnyContent] = Action { implicit request =>
    Ok(welcomePage(emailForm))
  }

  def sendEmail(): Action[AnyContent] = Action.async { implicit request =>
    emailForm.bindFromRequest().fold(
      formWithError => Future.successful(BadRequest(welcomePage(formWithError))),
      emailData => sendHomeEmail(emailData).map { sent =>
        Redirect(controllers.gathr.culpinteam.v1.routes.Application.welcome).flashing("success" -> "Thanks for your feedback")
      }
    )
  }

  def sendHomeEmail(email: Email): Future[Boolean] =
    EmailManager.sendHomeEmail(email.firstname, email.email, email.body)

  def testFacebookEvent(): Action[AnyContent] = TODO

}