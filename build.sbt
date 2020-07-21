lazy val V = new {
  val scalaVersion = "2.13.3"
}

lazy val pagination = project
  .in(file("modules/pagination"))
  .settings(
    name := "caliban-pagination"
  )

lazy val filter = project
  .in(file("modules/filter"))
  .settings(
    name := "caliban-filter"
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(pagination, filter)
  .settings(
    name := "caliban-extras-examples"
  )

lazy val root = project
  .in(file("."))
  .aggregate(pagination, filter, examples)
  .settings(
    name := "caliban-extras",
    organization := "com.github.niqdev",
    scalaVersion := V.scalaVersion,
    crossScalaVersions := Seq("2.12.12", V.scalaVersion)
  )
