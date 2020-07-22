lazy val V = new {
  val scalaVersion = "2.13.3"

  // examples
  val catsCore   = "2.1.1"
  val catsEffect = "2.1.4"
  val http4s     = "0.21.6"
  val doobie     = "0.9.0"
  val flyway     = "6.5.2"
  val caliban    = "0.9.0"
  val newtype    = "0.4.4"
  val refined    = "0.9.15"
  val logEffect  = "0.13.1"
  val logback    = "1.2.3"

  // test
  val scalatest  = "3.2.0"
  val scalacheck = "1.14.3"
}

lazy val commonSettings = Seq(
  organization := "com.github.niqdev",
  scalaVersion := V.scalaVersion,
  crossScalaVersions := Seq("2.12.12", V.scalaVersion),
  scalacOptions ++= Seq(
    "-Ymacro-annotations"
  )
)

lazy val core = project
  .in(file("modules/core"))
  .settings(commonSettings)
  .settings(
    name := "caliban-extras-core"
  )

lazy val doobie = project
  .in(file("modules/doobie"))
  .settings(commonSettings)
  .settings(
    name := "caliban-extras-doobie"
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(core, doobie)
  .settings(commonSettings)
  .settings(
    name := "caliban-extras-examples",
    libraryDependencies ++= Seq(
      "org.typelevel"         %% "cats-core"           % V.catsCore,
      "org.typelevel"         %% "cats-effect"         % V.catsEffect,
      "org.http4s"            %% "http4s-dsl"          % V.http4s,
      "org.http4s"            %% "http4s-blaze-server" % V.http4s,
      "org.tpolecat"          %% "doobie-core"         % V.doobie,
      "org.tpolecat"          %% "doobie-refined"      % V.doobie,
      "org.tpolecat"          %% "doobie-h2"           % V.doobie,
      "org.flywaydb"           % "flyway-core"         % V.flyway,
      "com.github.ghostdogpr" %% "caliban"             % V.caliban,
      "com.github.ghostdogpr" %% "caliban-http4s"      % V.caliban,
      "com.github.ghostdogpr" %% "caliban-cats"        % V.caliban,
      "io.estatico"           %% "newtype"             % V.newtype,
      "eu.timepit"            %% "refined"             % V.refined,
      "io.laserdisc"          %% "log-effect-fs2"      % V.logEffect,
      "ch.qos.logback"         % "logback-classic"     % V.logback    % Runtime,
      "org.scalatest"         %% "scalatest"           % V.scalatest  % Test,
      "org.scalacheck"        %% "scalacheck"          % V.scalacheck % Test
    )
  )

lazy val root = project
  .in(file("."))
  .aggregate(core, doobie, examples)
  .settings(
    name := "caliban-extras",
    addCommandAlias("checkFormat", ";scalafmtCheckAll;scalafmtSbtCheck"),
    addCommandAlias("format", ";scalafmtAll;scalafmtSbt"),
    addCommandAlias("build", ";checkFormat;clean;test")
  )
