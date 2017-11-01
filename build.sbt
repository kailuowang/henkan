import sbtrelease.ReleaseStateTransformations._

lazy val libModuleSettings = Common.settings ++ Dependencies.settings ++ publishSettings ++ Format.settings

lazy val henkan = project.in(file("."))
  .settings(moduleName := "henkan-all")
  .aggregate(convert, optional, examples, docs)
  .settings(Common.settings)
  .settings(publishSettings)
  .settings(Dependencies.commonSettings)
  .settings(Common.noPublishing)

lazy val convert = project
  .settings(moduleName := "henkan-convert")
  .settings(libModuleSettings:_*)
  .settings(libraryDependencies ++= Dependencies.shapeless)


lazy val optional = project
  .settings(moduleName := "henkan-optional")
  .settings(libraryDependencies ++= Dependencies.shapeless ++ Dependencies.cats)
  .settings(libModuleSettings)


lazy val examples = project
  .dependsOn(convert, optional)
  .aggregate(convert, optional)
  .settings(moduleName := "henkan-examples")
  .settings(Common.settings)
  .settings(Dependencies.settings)
  .settings(Common.noPublishing)
  .settings(Format.settings)
  .settings(
    libraryDependencies += "com.typesafe" % "config" % "1.3.0"
  )

lazy val docs = project
  .dependsOn(convert,optional)
  .settings(compile := (compile in Compile).dependsOn(tut).value)
  .settings(test := (test in Test).dependsOn(tut).value)
  .settings(moduleName := "henkan-docs")
  .settings(Dependencies.settings)
  .settings(tutSettings)
  .settings(tutScalacOptions ~= (_.filterNot(Set("-Ywarn-unused-import", "-Ywarn-dead-code"))))
  .settings(tutTargetDirectory := file("."))
  .settings(Common.noPublishing)

lazy val publishSettings = Seq(
//  sonatypeProfileName := "kailuowang",
  organization in ThisBuild := "com.kailuowang",
  publishMavenStyle := true,
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("http://kailuowang.github.io/henkan")),
  scmInfo := Some(ScmInfo(url("https://github.com/kailuowang/henkan"),
    "git@github.com:kailuowang/henkan.git", Some("git@github.com:kailuowang/henkan.git"))),
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
    pushChanges)
)
