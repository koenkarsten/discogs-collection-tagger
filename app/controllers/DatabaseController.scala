package controllers

import play.api.libs.json.{JsString, JsArray}
import play.api.libs.ws.WSClient

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Props, Actor}
import controllers.CollectionController.CollectionQueue
import controllers.Models.{Release, Track}

object DatabaseController {
  def props(ws: WSClient) = Props(classOf[DatabaseController], ws)
  case object GetQueueMessage
}

class DatabaseController(ws: WSClient) extends Actor {
  import DatabaseController._

  val requests = new ApiController(ws)
  val queue = mutable.Queue[Release]()
  val storage = Storage
  context.system.scheduler.schedule(500 millis, 500 millis) { processRelease() }

  def processRelease(): Unit = {
    if(queue.nonEmpty) {
      var release = queue.dequeue()
      val response = requests.getResource(release.resourceURL)
      println(s"getting $release from Queue, ${queue.size} remaining.")

      val styles = (response.json \ "styles").getOrElse(JsArray()).as[ListBuffer[String]]
      val genres = (response.json \ "genres").getOrElse(JsArray()).as[ListBuffer[String]]
      release = release.copy(styles = styles, genres = genres)

      val tracks = (response.json \ "tracklist").getOrElse(JsArray()).as[JsArray]
      for(t <- tracks.value) {
        val position = (t \ "position").getOrElse(JsString("")).as[String]
        val title = (t \ "title").getOrElse(JsString("")).as[String]

        release.tracks += Track(None, release.id, position, title, "")
      }

      storage.saveRelease(release)
    }
  }

  var allowed = List(219517, 7573431, 3729226, 5726711, 8154672) // For debugging
  def queueReleases(cq: CollectionQueue): Unit = queue ++= cq.user.catalogue.toList //.filter(r => allowed.contains(r.id))
  def getQueueSize: Int = queue.size
  def receive = {
    case cq: CollectionQueue => queueReleases(cq)
    case GetQueueMessage => sender() ! getQueueSize
  }

}