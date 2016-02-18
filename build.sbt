
lazy val henkan = project.in(file("."))
  .settings(moduleName := "root")
  .aggregate(core, tests)
  .settings(Common.settings:_*)
  .settings(Common.noPublishing: _*)

lazy val core = project.in(file("core"))
  .settings(moduleName := "henkan-core")
  .settings(Common.settings:_*)
  .settings(Dependencies.settings:_*)
  .settings(Publish.settings:_*)
  .settings(Format.settings:_*)

lazy val tests = project.in(file("tests"))
  .dependsOn(core)
  .aggregate(core)
  .settings(moduleName := "henkan-tests")
  .settings(Common.settings:_*)
  .settings(Common.noPublishing: _*)
  .settings(Dependencies.testSettings:_*)
  .settings(Format.settings:_*)
  .settings(Testing.settings:_*)

lazy val examples = project.in(file("examples"))
  .dependsOn(core)
  .aggregate(core)
  .settings(moduleName := "henkan-examples")
  .settings(Common.settings:_*)
  .settings(Dependencies.settings:_*)
  .settings(Common.noPublishing: _*)
  .settings(Format.settings:_*)

lazy val docs = project.in(file("docs"))
  .dependsOn(core)
  .settings(moduleName := "henkan-docs")
  .settings(Dependencies.settings:_*)
  .settings(tutSettings)
  .settings(tutScalacOptions ~= (_.filterNot(Set("-Ywarn-unused-import", "-Ywarn-dead-code"))))
  .settings(tutTargetDirectory := file("."))
  .settings(Common.noPublishing: _*)
