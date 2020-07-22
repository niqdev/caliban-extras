lazy val V = new {
  val scalaVersion = "2.13.3"
}

lazy val core = project
  .in(file("modules/core"))
  .settings(
    name := "caliban-extras-core"
  )

lazy val doobie = project
  .in(file("modules/doobie"))
  .settings(
    name := "caliban-extras-doobie"
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(core, doobie)
  .settings(
    name := "caliban-extras-examples"
  )

lazy val root = project
  .in(file("."))
  .aggregate(core, doobie, examples)
  .settings(
    name := "caliban-extras",
    organization := "com.github.niqdev",
    scalaVersion := V.scalaVersion,
    crossScalaVersions := Seq("2.12.12", V.scalaVersion),
    addCommandAlias("checkFormat", ";scalafmtCheckAll;scalafmtSbtCheck"),
    addCommandAlias("format", ";scalafmtAll;scalafmtSbt"),
    addCommandAlias("build", ";checkFormat;clean;test")
  )
