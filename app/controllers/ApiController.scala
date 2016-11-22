package controllers

import play.api.libs.ws.{WSResponse, WSClient, WSRequest}
import play.api.mvc.Controller

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


class ApiController(ws: WSClient, duration: FiniteDuration = 10 seconds) extends Controller {

  def getResource(url: String): WSResponse = {
    val request: WSRequest = ws.url(url)
    val future: Future[WSResponse] = request.get().map(response => response)

    Await.result(future, duration)
  }
}

