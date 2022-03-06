lazy val V = new {
  val caliban    = "0.9.5"
  val catsCore   = "2.4.2"
  val catsEffect = "2.3.3"
  val doobie     = "0.10.0"
  val enumeratum = "1.6.1"
  val flyway     = "7.5.4"
  val http4s     = "0.21.19"
  val newtype    = "0.4.4"
  val logEffect  = "0.14.1"
  val logback    = "1.2.11"
  val refined    = "0.9.21"
  val zio        = "1.0.4-2"

  // test
  val scalacheck = "1.15.3"
  val scalatest  = "3.2.5"
}

lazy val commonSettings = Seq(
  organization := "com.github.niqdev",
  crossScalaVersions := List("2.12.12", "2.13.5"),
  scalaVersion := "2.13.5",
  scalacOptions ++= PartialFunction
    .condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
      case Some((2, v)) if v >= 13 =>
        Seq("-Ymacro-annotations")
      case Some((2, v)) if v <= 12 =>
        Seq("-Xsource:2.13")
    }
    .toList
    .flatten,
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
      case _ =>
        Seq.empty
    }
  }
)

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/niqdev/caliban-extras")),
  licenses := List("MIT" -> url("https://github.com/niqdev/caliban-extras/blob/master/LICENSE")),
  scmInfo := Some(
    ScmInfo(
      url(s"https://github.com/niqdev/caliban-extras"),
      "scm:git:git@github.com:niqdev/caliban-extras.git"
    )
  ),
  developers := List(
    Developer("niqdev", "niqdev", "niqdev@users.noreply.github.com", url("https://github.com/niqdev"))
  )
)

lazy val disablePublishSettings = Seq(
  skip in publish := true,
  publishArtifact := false
)

lazy val refined = project
  .in(file("modules/refined"))
  .settings(commonSettings)
  .settings(publishSettings)
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
  .settings(disablePublishSettings)
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
  .settings(disablePublishSettings)
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
  .settings(disablePublishSettings)
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
      "ch.qos.logback"         % "logback-classic"     % V.logback % Runtime
    )
  )

lazy val root = project
  .in(file("."))
  .aggregate(core, doobie, refined, examples)
  .settings(disablePublishSettings)
  .settings(
    name := "caliban-extras",
    addCommandAlias("checkFormat", ";scalafmtCheckAll;scalafmtSbtCheck"),
    addCommandAlias("format", ";scalafmtAll;scalafmtSbt"),
    addCommandAlias("build", ";checkFormat;clean;test")
  )
