package controllers

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Props, Actor}
import controllers.CollectionController.CollectionQueue
import controllers.Models.Release

object DatabaseController {
  def props = Props[DatabaseController]
}

class DatabaseController extends Actor {
  val stack = mutable.Stack[Release]()
  def queueReleases(cq: CollectionQueue): Unit = stack.pushAll(cq.queue.toList)
  def processRelease(): Unit = if(stack.nonEmpty) println(s"getting ${stack.pop()} from Stack, ${stack.size} remaining")

  context.system.scheduler.schedule(500 millis, 500 millis) { processRelease() }

  def receive = {
    case cq: CollectionQueue => queueReleases(cq)
  }

}