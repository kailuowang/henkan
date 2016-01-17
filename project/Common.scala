import sbt.Keys._
import sbt._

object Common {
  val noPublishing = Seq(publish := (), publishLocal := (), publishArtifact := false)

  val settings = Seq(
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-Xlint"
    )
  )
}
