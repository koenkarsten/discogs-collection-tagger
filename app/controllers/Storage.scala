package controllers

import io.getquill._
import Models._

import scala.collection.mutable.ListBuffer

object Storage {
  case class Status()
  object Success extends Status
  object Failure extends Status

  val ctx = new JdbcContext[MySQLDialect, Literal with MysqlEscape]("ctx")
  import ctx._
  implicit val listbufferEncoder: Encoder[ListBuffer[String]] =
    encoder[ListBuffer[String]](row => (id, listBuffer) => row.setObject(id, listBuffer.mkString("##")), java.sql.Types.VARCHAR)
  implicit val listbufferDecoder: Decoder[ListBuffer[String]] =
    decoder[ListBuffer[String]](row => index => ListBuffer[String](row.getString(index).split("##"): _*) )
  implicit val trackDecoder: Decoder[ListBuffer[Track]] =
    decoder[ListBuffer[Track]](row => index => ListBuffer[Track](Track(None, 0, "", "", "")))

  def getReleaseByUsername(username: String): List[Release] = {
    val q = quote {
      for {
        r <- query[Release]
      } yield r
    }

    ctx.run(q)
  }

  def saveUser(user: User): Status = {
    try {
      val q = quote {
        query[User].insert(_.username -> lift(user.username))
      }
      ctx.run(q)
      Success
    } catch {
      case e: Throwable =>
        println(s"${Console.RED} Quill encountered: $e ${Console.RESET}")
        Failure
    }


  }

  def saveRelease(release: Release): Status = {
    def updateRelease: Status = {
      try {
        ctx.transaction {
          val rQuery = quote(query[Release].filter(_.id == lift(release.id)).update(
            _.id -> lift(release.id),
            _.owner -> lift(release.owner),
            _.artists -> lift(release.artists),
            _.name -> lift(release.name),
            _.resourceURL -> lift(release.resourceURL),
            _.styles -> lift(release.styles),
            _.genres -> lift(release.genres)
          ))
          ctx.run(rQuery)

          release.tracks.toList.filter(t => t.position.nonEmpty).foreach { t =>
            val tQuery = quote(query[Track].filter(_.releaseID == lift(t.releaseID)).filter(_.position == lift(t.position)).update(
              _.releaseID -> lift(t.releaseID),
              _.position -> lift(t.position),
              _.title -> lift(t.title)
            ))
            ctx.run(tQuery)
          }
        }

        println(s"\tUpdate Succeeded")
        Success
      } catch {
        case e: Throwable =>
          println(Console.RED + s"\tUpdate failed: $e" + Console.RESET)
          Failure
      }
    }

    try {
      ctx.transaction {
        val rQuery = quote(query[Release].insert(
          _.id -> lift(release.id),
          _.owner -> lift(release.owner),
          _.artists -> lift(release.artists),
          _.name -> lift(release.name),
          _.resourceURL -> lift(release.resourceURL),
          _.styles -> lift(release.styles),
          _.genres -> lift(release.genres)
        ))
        ctx.run(rQuery)

        release.tracks.toList.filter(t => t.position.nonEmpty).foreach { t =>
          val tQuery = quote(query[Track].insert(
            _.releaseID -> lift(t.releaseID),
            _.position -> lift(t.position),
            _.title -> lift(t.title)
          ))
          ctx.run(tQuery)
        }
      }

      println(s"\tInsert Succeeded")
      Success
    } catch {
      case e: Throwable =>
        println(s"\tInsert failed, trying update instead")
        updateRelease
    }
  }
}
