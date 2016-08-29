lazy val myProject = (project in file(".")).enablePlugins(JavaAppPackaging, SbtTwirl)

name := "tombot"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8", "-language:postfixOps")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.8",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.8",
  "net.virtual-void" %%  "json-lenses" % "0.6.1",
  "com.google.inject" % "guice" % "4.1.0",
  "net.codingwell" %% "scala-guice" % "4.0.1",
  "com.ibm.watson.developer_cloud" % "java-sdk" % "3.3.0",
  "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.4",
  "commons-codec" % "commons-codec" % "1.3",
  "org.apache.httpcomponents" % "httpclient" % "4.0.1",
  "org.apache.httpcomponents" % "httpcore" % "4.0.1",
  "commons-logging" % "commons-logging" % "1.2",
  "btomala" %% "akka-http-twirl" % "1.1.0" excludeAll
    ExclusionRule(organization = "com.typesafe.akka")
)

unmanagedBase <<= baseDirectory { base => base / "libs" }

Revolver.settings

resolvers += "Bartek's repo at Bintray" at "https://dl.bintray.com/btomala/maven"
