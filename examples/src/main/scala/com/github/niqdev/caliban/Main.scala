package com.github.niqdev.caliban

import caliban.Http4sAdapter
import caliban.interop.cats.implicits.CatsEffectGraphQL
import cats.effect.{ ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer }
import com.github.niqdev.caliban.repositories.Repositories
import com.github.niqdev.caliban.resolvers.Resolvers
import com.github.niqdev.caliban.services.Services
import log.effect.LogWriter
import log.effect.fs2.SyncLogWriter.log4sLog
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli.http4sKleisliResponseSyntaxOptionT
import zio.Runtime

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  private[this] implicit val runtime: Runtime[Any] = Runtime.default

  override def run(args: List[String]): IO[ExitCode] =
    log4sLog[IO](getClass)
      .flatMap(implicit logger =>
        server[IO]
          .use(_ => IO.never)
          .as(ExitCode.Success)
      )

  private[caliban] def server[F[_]: ConcurrentEffect: ContextShift: Timer: LogWriter]: Resource[F, Unit] =
    for {
      _            <- Resource.liftF(LogWriter.info("Start server..."))
      xa           <- database.initH2[F]
      repositories <- Repositories.make[F](xa)
      services     <- Services.make[F](repositories)
      api          <- Resolvers.make[F](services)
      _            <- Resource.liftF(LogWriter.info(s"GraphQL Schema:\n${api.render}"))
      interpreter  <- Resource.liftF(api.interpreterAsync)
      httpApp = Router(
        "/api/graphql" -> Http4sAdapter.makeHttpServiceF(interpreter)
      ).orNotFound
      _ <- BlazeServerBuilder[F](ExecutionContext.global)
        .bindLocal(8080)
        .withHttpApp(httpApp)
        .resource
    } yield ()
}
