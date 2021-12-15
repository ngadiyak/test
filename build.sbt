name := "matcher-ws-long-test"

version := "0.1"

scalaVersion := "2.13.7"

val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "com.softwaremill.sttp.client3" %% "core" % "3.3.18",
  "com.softwaremill.sttp.client3" %% "play-json" % "3.3.18",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.3.18"
)