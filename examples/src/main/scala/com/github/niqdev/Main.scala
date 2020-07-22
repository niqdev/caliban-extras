package com.github.niqdev

import cats.effect.{ ExitCode, IO, IOApp }
import log.effect.fs2.SyncLogWriter.log4sLog

// sbt -jvm-debug 5005 "examples/runMain com.github.niqdev.Main"
object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    log4sLog[IO](getClass).flatMap(_.info("hello")) *>
      IO.pure(ExitCode.Success)
}
