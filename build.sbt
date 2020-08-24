lazy val V = new {
  val caliban    = "0.9.1"
  val catsCore   = "2.1.1"
  val catsEffect = "2.1.4"
  val doobie     = "0.9.0"
  val enumeratum = "1.6.1"
  val flyway     = "6.5.3"
  val http4s     = "0.21.7"
  val newtype    = "0.4.4"
  val logEffect  = "0.13.1"
  val logback    = "1.2.3"
  val refined    = "0.9.15"
  val zio        = "1.0.1"

  // test
  val scalacheck = "1.14.3"
  val scalatest  = "3.2.1"
}

lazy val commonSettings = Seq(
  organization := "com.github.niqdev",
  scalaVersion := "2.13.3",
  scalacOptions ++= Seq(
    "-Ymacro-annotations"
  )
)

lazy val refined = project
  .in(file("modules/refined"))
  .settings(commonSettings)
  .settings(
    name := "caliban-refined",
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    libraryDependencies ++= Seq(
      "com.github.ghostdogpr" %% "caliban"      % V.caliban,
      "eu.timepit"            %% "refined"      % V.refined,
      "io.estatico"           %% "newtype"      % V.newtype,
      "dev.zio"               %% "zio-test"     % V.zio % Test,
      "dev.zio"               %% "zio-test-sbt" % V.zio % Test
    )
  )

lazy val core = project
  .in(file("modules/core"))
  .dependsOn(refined)
  .settings(commonSettings)
  .settings(
    name := "caliban-extras-core",
    libraryDependencies ++= Seq(
      "org.typelevel"         %% "cats-core"    % V.catsCore,
      "org.typelevel"         %% "cats-effect"  % V.catsEffect,
      "com.github.ghostdogpr" %% "caliban"      % V.caliban,
      "com.github.ghostdogpr" %% "caliban-cats" % V.caliban,
      "org.scalatest"         %% "scalatest"    % V.scalatest  % Test,
      "org.scalacheck"        %% "scalacheck"   % V.scalacheck % Test
    )
  )

lazy val doobie = project
  .in(file("modules/doobie"))
  .settings(commonSettings)
  .settings(
    name := "caliban-extras-doobie",
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core"    % V.doobie,
      "org.tpolecat" %% "doobie-refined" % V.doobie,
      "org.tpolecat" %% "doobie-h2"      % V.doobie,
      "org.flywaydb"  % "flyway-core"    % V.flyway
    )
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(core, doobie)
  .settings(commonSettings)
  .settings(
    name := "caliban-extras-examples",
    libraryDependencies ++= Seq(
      "org.http4s"            %% "http4s-dsl"          % V.http4s,
      "org.http4s"            %% "http4s-blaze-server" % V.http4s,
      "com.github.ghostdogpr" %% "caliban-http4s"      % V.caliban,
      "com.beachape"          %% "enumeratum"          % V.enumeratum,
      "com.beachape"          %% "enumeratum-circe"    % V.enumeratum,
      "com.beachape"          %% "enumeratum-doobie"   % "1.6.0",
      "io.laserdisc"          %% "log-effect-fs2"      % V.logEffect,
      "ch.qos.logback"         % "logback-classic"     % V.logback    % Runtime,
      "org.scalatest"         %% "scalatest"           % V.scalatest  % Test,
      "org.scalacheck"        %% "scalacheck"          % V.scalacheck % Test
    )
  )

lazy val root = project
  .in(file("."))
  .aggregate(core, doobie, refined, examples)
  .settings(
    name := "caliban-extras",
    addCommandAlias("checkFormat", ";scalafmtCheckAll;scalafmtSbtCheck"),
    addCommandAlias("format", ";scalafmtAll;scalafmtSbt"),
    addCommandAlias("build", ";checkFormat;clean;test")
  )
