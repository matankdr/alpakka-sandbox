name := "alpakka-sandbox"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "1.1.1",
  "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.5",
  "io.kamon" %% "kamon-core" % "2.0.0"
)