package controllers

import akka.actor.ActorSystem
import com.google.inject.Inject
import play.api.libs.ws.{WSResponse, WSClient, WSRequest}
import play.api.mvc.Controller

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object RequestLimiter {
  trait Answer
  case class Success() extends Answer
  case class Fail() extends Answer

  object Lock {
    var locked = false
    def unlock() = locked = false
    def lock() = {
      if (!locked) {
        locked = true
        Success
      } else Fail
    }
  }
  case object Granter {
    trait Grant
    case class ApprovedGrant(id: Int) extends Grant
    case class DisapprovedGrant() extends Grant

    var lock = Lock
    var grantCounter = 0

    def claim(): Grant = {
      lock.lock() match {
        case Success => ApprovedGrant(grantCounter)
        case Fail => DisapprovedGrant()
      }
    }
    def unclaim(grand: ApprovedGrant): Unit = {
      if(grand.id == grantCounter) {
        grantCounter += 1
        lock.unlock()
      }
    }
  }
  object CreditPool {
    var pool = 240
    var lock = Lock
    var granter = Granter

    def increment(amount: Int = 1): Answer = {
      granter.claim() match {
        case ag: Granter.ApprovedGrant =>
          pool += amount
          granter.unclaim(ag)
          Success()
        case dg: Granter.DisapprovedGrant => Fail()
      }
    }
    def decrement(amount: Int = 1): Answer = {
      granter.claim() match {
        case ag: Granter.ApprovedGrant =>
          pool -= amount
          granter.unclaim(ag)
          Success()
        case dg: Granter.DisapprovedGrant => Fail()
      }
    }
  }

  var credits = CreditPool
}

class RequestController @Inject()(ws: WSClient, system: ActorSystem)(duration: FiniteDuration = 10 seconds) extends Controller {
  var limiter = RequestLimiter
  system.scheduler.schedule(0 seconds, 1 seconds) {
    limiter.credits.increment(4)
    println("Pool: " + limiter.credits.pool.toString)
  }

  def getResource(url: String): WSResponse = {
    limiter.credits.decrement(1) match {
      case s: RequestLimiter.Success =>
        val request: WSRequest = ws.url(url)
        val future: Future[WSResponse] = request.get().map(response => response)
        val response = Await.result(future, duration)

        response
      case f: RequestLimiter.Fail => getResource(url)
    }
  }

}
