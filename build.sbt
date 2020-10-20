import Dependencies._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "tafit"
ThisBuild / organizationName := "tafit3"

lazy val root = (project in file("."))
  .settings(
    name := "never",
    libraryDependencies ++= scalaTest ++ jackson ++ Seq(typesafeConfig, commonsIo)
  )

