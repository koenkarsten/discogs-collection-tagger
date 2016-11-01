package controllers

import akka.actor.ActorSystem

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.concurrent.duration._
import com.google.inject.Inject
import play.api.libs.ws.{WSRequest, WSClient}
import play.api.mvc.{Action, Controller}
import play.api.libs.json.{JsString, JsNumber, JsArray, JsValue}
import controllers.Models.Release

object CollectionController {
  def randomUUID = java.util.UUID.randomUUID.toString
  case class CollectionQueue(uuid: String, username: String, queue: ListBuffer[Release]) {
    override def toString = {
      var r = s"Queue $username $uuid\n"
      queue.toList.foreach(release => r += s"\t$release\n")
      r
    }
  }
}

class CollectionController @Inject()(implicit context: ExecutionContext, ws: WSClient, system: ActorSystem) extends Controller {
  import CollectionController._
  val storage = system.actorOf(DatabaseController.props)

  def setupImport = Action {
    Ok(views.html.setup())
  }

  def confirmImport(username: String) = Action {
    Ok(views.html.importCollection(username))
  }

  def fillQueue(queue: CollectionQueue, page: Int): CollectionQueue = {
    val request: WSRequest = ws.url(s"https://api.discogs.com/users/${queue.username}/collection?per_page=100&page=$page")
    val futureResponse: Future[JsValue] = request.get().map(response => response.json)
    val response = Await.result(futureResponse, 10 seconds)
    var queueCopy = queue.copy()

    val releases = (response \ "releases").getOrElse(JsArray()).as[JsArray]
    for(r <- releases.value) {
      val id = (r \ "basic_information" \ "id").getOrElse(JsNumber(0)).as[Int]
      val name = (r \ "basic_information" \ "title").getOrElse(JsString("")).as[String]
      val url = (r \ "basic_information" \ "resource_url").getOrElse(JsString("")).as[String]
      queueCopy.queue += Release(id, name, url, None)
    }

    val pages = (response \ "pagination" \ "pages").getOrElse(JsNumber(1)).as[Int]
    println(s"Processed page $page / $pages")
    if(page < pages) {
      queueCopy = fillQueue(queueCopy, page + 1)
    }

    queueCopy
  }

  def startImport(username: String) = Action {
    val emptyQueue = CollectionQueue(randomUUID, username,  ListBuffer.empty[Release])
    val filledQueue = fillQueue(emptyQueue, 1)

    storage ! filledQueue
    Ok(filledQueue.toString)
  }

}
