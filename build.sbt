import sbtrelease.ReleaseStateTransformations._

lazy val libModuleSettings = Common.settings ++ Dependencies.settings ++ publishSettings ++ Format.settings

lazy val henkan = project.in(file("."))
  .settings(moduleName := "henkan-all")
  .aggregate(extract, convert, k, examples, docs)
  .settings(Common.settings:_*)
  .settings(Common.noPublishing: _*)

lazy val extract = project
  .settings(moduleName := "henkan-extract")
  .settings(libModuleSettings:_*)
  .settings(Dependencies.withKittens:_*)

lazy val convert = project
  .settings(moduleName := "henkan-convert")
  .settings(libModuleSettings:_*)
  .settings(libraryDependencies ++= Dependencies.shapeless)

lazy val k = project
  .settings(moduleName := "henkan-k")
  .settings(Dependencies.withKittens:_*)
  .settings(libModuleSettings:_*)


lazy val optional = project
  .settings(moduleName := "henkan-optional")
  .settings(Dependencies.withKittens:_*)
  .settings(libModuleSettings:_*)


lazy val examples = project
  .dependsOn(extract, convert, k, optional)
  .aggregate(extract, convert, k, optional)
  .settings(moduleName := "henkan-examples")
  .settings(Common.settings:_*)
  .settings(Dependencies.settings:_*)
  .settings(Dependencies.withKittens:_*)
  .settings(Common.noPublishing: _*)
  .settings(Format.settings:_*)
  .settings(
    libraryDependencies += "com.typesafe" % "config" % "1.3.0"
  )

lazy val docs = project
  .dependsOn(extract, convert, k, optional)
  .settings(compile := (compile in Compile).dependsOn(tut).value)
  .settings(test := (test in Test).dependsOn(tut).value)
  .settings(moduleName := "henkan-docs")
  .settings(Dependencies.settings:_*)
  .settings(tutSettings)
  .settings(tutScalacOptions ~= (_.filterNot(Set("-Ywarn-unused-import", "-Ywarn-dead-code"))))
  .settings(tutTargetDirectory := file("."))
  .settings(Common.noPublishing: _*)

lazy val publishSettings = Seq(
//  sonatypeProfileName := "kailuowang",
  organization in ThisBuild := "com.kailuowang",
  publishMavenStyle := true,
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("http://kailuowang.github.io/henkan")),
  scmInfo := Some(ScmInfo(url("https://github.com/kailuowang/henkan"),
    "git@github.com:kailuowang/henkan.git")),
  pomIncludeRepository := { _ => false },
  publishArtifact in Test := false,
  publishMavenStyle := true,
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  pomExtra := (
    <developers>
      <developer>
        <id>kailuowang</id>
        <name>Kailuo Wang</name>
        <email>kailuo.wang@gmail.com</email>
      </developer>
    </developers>
    ),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )

)
