package controllers

object Models {
  case class Release(id: Int, name: String, resourceURL: String, tracks: Option[List[Track]])
  case class Track(id: Int, name: String, genres: List[String], labels: Option[List[Label]])
  case class Label(id: Int, name: String)
}
