package controllers

import scala.concurrent.ExecutionContext

import com.google.inject.Inject
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

class CollectionController @Inject()(implicit context: ExecutionContext, ws: WSClient) extends Controller {

  def confirmImport(username: String) = Action {
    Ok(views.html.importCollection(username))
  }

  def startImport = Action {
    Ok("Importing...")
  }

}
