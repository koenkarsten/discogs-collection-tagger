name := "discogs"

version := "1.0"

lazy val `discogs` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= {
  Seq(
    jdbc,
    cache,
    ws,
    specs2 % Test,
    "mysql" % "mysql-connector-java" % "5.1.38",
    "io.getquill" %% "quill-jdbc" % "1.0.0")
}

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"