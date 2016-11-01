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
}

class DatabaseController(ws: WSClient) extends Actor {
  context.system.scheduler.schedule(500 millis, 500 millis) { processRelease() }
  val requests = new RequestController(ws)
  val queue = mutable.Queue[Release]()

  def queueReleases(cq: CollectionQueue): Unit = queue ++= cq.queue.toList
  def processRelease(): Unit = {
    if(queue.nonEmpty) {
      val release = queue.dequeue()
      val response = requests.getJsonResource(release.resourceURL)
      println(s"getting $release from Queue, ${queue.size} remaining")

      val styles = (response \ "styles").getOrElse(JsArray()).as[List[String]]
      val genres = (response \ "genres").getOrElse(JsArray()).as[List[String]]
      val combined = styles ::: genres

      println(s"\t $combined")
    }
  }

  def receive = {
    case cq: CollectionQueue => queueReleases(cq)
  }

}