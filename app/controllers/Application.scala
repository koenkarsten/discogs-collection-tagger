package controllers

import scala.concurrent.ExecutionContext
import play.api.mvc.Controller
import play.api.libs.ws.WSClient
import com.google.inject.Inject

class Application @Inject()(implicit context: ExecutionContext, ws: WSClient) extends Controller {

}