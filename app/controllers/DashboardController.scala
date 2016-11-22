package controllers

import play.api.mvc.{Action, Controller}

class DashboardController extends Controller {
  def home(username: String) = Action {
    val storage = Storage
    val releases = storage.getReleaseByUsername(username)
    Ok(views.html.dashboard(username, releases))
  }
}