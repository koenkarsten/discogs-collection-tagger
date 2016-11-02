package controllers

import play.api.libs.json.JsArray
import play.api.libs.ws.WSClient

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Props, Actor}
import controllers.CollectionController.CollectionQueue
import controllers.Models.Release

object DatabaseController {
  def props(ws: WSClient) = Props(classOf[DatabaseController], ws)
  case object GetQueueMessage
}

class DatabaseController(ws: WSClient) extends Actor {
  import DatabaseController._

  context.system.scheduler.schedule(500 millis, 500 millis) { processRelease() }
  val requests = new ApiController(ws)
  val queue = mutable.Queue[Release]()

  def queueReleases(cq: CollectionQueue): Unit = queue ++= cq.queue.toList
  def processRelease(): Unit = {
    if(queue.nonEmpty) {
      val release = queue.dequeue()
      val response = requests.getResource(release.resourceURL)
      println(s"getting $release from Queue, ${queue.size} remaining.")

      val styles = (response.json \ "styles").getOrElse(JsArray()).as[List[String]]
      val genres = (response.json \ "genres").getOrElse(JsArray()).as[List[String]]
      val combined = (styles ::: genres).mkString("##")

      println(s"\t $combined")
    }
  }

  def getQueueSize: Int = queue.size

  def receive = {
    case cq: CollectionQueue => queueReleases(cq)
    case GetQueueMessage => sender() ! getQueueSize
  }

}