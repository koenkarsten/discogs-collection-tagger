package controllers

import play.api.libs.json.JsValue
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class RequestController(ws: WSClient, duration: FiniteDuration = 10 seconds) {

  def getJsonResource(url: String): JsValue = {
    val request: WSRequest = ws.url(url)
    println( request.options )
    val future: Future[JsValue] = request.get().map(response => response.json)
    val response = Await.result(future, duration)

    response
  }

}
