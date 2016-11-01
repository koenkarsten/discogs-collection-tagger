package controllers

import play.api.libs.json.{JsString, JsNumber, JsArray, JsValue}

import scala.concurrent.{Future, ExecutionContext}
import play.api.mvc.{Action, Controller}
import play.api.libs.ws.{WSRequest, WSClient}
import com.google.inject.Inject

import controllers.Models.Release

class Application @Inject()(implicit context: ExecutionContext, ws: WSClient) extends Controller {

  def api(username: String) = Action.async {
    val request: WSRequest = ws.url(s"https://api.discogs.com/users/$username/collection?per_page=100&page=1")
    val futureResponse: Future[JsValue] = request.get().map(response => response.json)

    futureResponse.map({ response =>
      val releases = (response \ "releases").getOrElse(JsArray()).as[JsArray]
      for(r <- releases.value) {
        val id = (r \ "basic_information" \ "id").getOrElse(JsNumber(0)).as[Int]
        val name = (r \ "basic_information" \ "title").getOrElse(JsString("")).as[String]
        println( Release(id, name, "", None) )
      }

      Ok(response)
    })

  }

}