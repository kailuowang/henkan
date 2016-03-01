import sbt._, Keys._
import bintray.BintrayKeys._


object Publish {

  val bintraySettings = Seq(
    bintrayOrganization := Some("kailuowang"),
    bintrayPackageLabels := Seq("henkan")
  )

  val publishingSettings = Seq(
    organization in ThisBuild := "com.kailuowang",
    publishMavenStyle := true,
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    homepage := Some(url("http://kailuowang.github.io/henkan")),
    scmInfo := Some(ScmInfo(url("https://github.com/kailuowang/henkan"),
      "git@github.com:kailuowang/henkan.git")),
    pomIncludeRepository := { _ => false },
    publishArtifact in Test := false
  )

  val settings = bintraySettings ++ publishingSettings
}

