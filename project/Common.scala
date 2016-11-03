import sbt.Keys._
import sbt._

object Common {
  val noPublishing = Seq(publish := (), publishLocal := (), publishArtifact := false)

  val settings = Seq(
    scalaVersion := "2.11.8",
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
//      "-Xlog-implicits",
      "-Xlint"
    )
  )
}
