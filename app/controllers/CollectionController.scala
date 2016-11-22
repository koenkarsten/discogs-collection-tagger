package controllers

import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContext}
import com.google.inject.Inject
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import play.api.libs.json.{JsString, JsNumber, JsArray}
import controllers.Models.{Track, Release, User}

object CollectionController {
  def randomUUID = java.util.UUID.randomUUID.toString

  case class CollectionQueue(uuid: String, user: User) {
    override def toString = {
      var r = s"Queue ${user.username} $uuid\n"
      user.catalogue.toList.foreach(release => r += s"\t$release\n")
      r
    }
  }
}

class CollectionController @Inject()(implicit context: ExecutionContext, ws: WSClient, system: ActorSystem) extends Controller {
  import CollectionController._
  implicit val timeout = Timeout(5 seconds)
  val storage = system.actorOf(DatabaseController.props(ws))
  val requests = new ApiController(ws)

  def setupImport = Action { Ok(views.html.setup()) }
  def confirmImport(username: String) = Action { Ok(views.html.importCollection(username)) }

  def fillQueue(queue: CollectionQueue, page: Int): CollectionQueue = {
    val response = requests.getResource(s"https://api.discogs.com/users/${queue.user.username}/collection?per_page=100&page=$page")
    var queueCopy = queue.copy()

    val releases = (response.json \ "releases").getOrElse(JsArray()).as[JsArray]
    for(r <- releases.value) {
      val id = (r \ "basic_information" \ "id").getOrElse(JsNumber(0)).as[Int]
      val name = (r \ "basic_information" \ "title").getOrElse(JsString("")).as[String]
      val url = (r \ "basic_information" \ "resource_url").getOrElse(JsString("")).as[String]
      queue.user.catalogue += Release(id, name, url, ListBuffer.empty[String], ListBuffer.empty[String], ListBuffer.empty[Track])
    }

    val pages = (response.json \ "pagination" \ "pages").getOrElse(JsNumber(1)).as[Int]
    println(s"Processed page $page / $pages")
    if(page < pages) {
      queueCopy = fillQueue(queueCopy, page + 1)
    }

    queueCopy
  }

  def startImport(username: String) = Action {
    val emptyQueue = CollectionQueue(randomUUID, User(username, ListBuffer.empty[Release]))
    val filledQueue = fillQueue(emptyQueue, 1)

    storage ! filledQueue
    Ok(views.html.importProgress(filledQueue, system.settings.config.getString("app.host")))
  }

  def getImportProgress = Action {
    val future = storage ? DatabaseController.GetQueueMessage
    val response = Await.result(future, timeout.duration).asInstanceOf[Int]
    Ok(response.toString)
  }

}