import sbt._

object Dependencies {
  val scalaTestVersion = "3.2.2"
  val jacksonVersion = "2.11.2"
  val mockitoScalaVersion = "1.16.0"

  lazy val scalaTest = Seq(
    "org.scalactic" %% "scalactic" % scalaTestVersion % "test",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test")
  lazy val mockitoScala = Seq(
    "org.mockito" %% "mockito-scala" % mockitoScalaVersion % "test",
    "org.mockito" %% "mockito-scala-scalatest" % mockitoScalaVersion % "test")
  lazy val jackson = Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
    "com.fasterxml.jackson.datatype" %"jackson-datatype-jsr310" % jacksonVersion)
  lazy val typesafeConfig = "com.typesafe" % "config" % "1.4.0"
  lazy val commonsIo = "commons-io" % "commons-io" % "2.8.0"
}
