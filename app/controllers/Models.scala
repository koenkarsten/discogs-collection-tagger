package controllers

import scala.collection.mutable.ListBuffer

object Models {
  case class User(username: String, catalogue: ListBuffer[Release])
  case class Release(id: Int, owner: String, artists: ListBuffer[String], name: String, resourceURL: String, styles: ListBuffer[String], genres: ListBuffer[String], tracks: ListBuffer[Track])
  case class Track(id: Option[Int], releaseID: Int, position: String, title: String, labels: String = "")
}
