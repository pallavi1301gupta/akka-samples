name := "akka-samples"

version := "0.0.1.0"

scalaVersion := "2.11.7"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.6"
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.5.5"

libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "0.13"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.5"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.6"

libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.0"

// Akka Test Dependecies
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % "10.0.10" % Test
