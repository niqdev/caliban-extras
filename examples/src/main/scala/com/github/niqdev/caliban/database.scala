package com.github.niqdev.caliban

import cats.effect.{ Async, Blocker, ContextShift, Resource, Sync }
import doobie.h2.H2Transactor
import doobie.util.ExecutionContexts
import log.effect.LogWriter
import org.flywaydb.core.Flyway

object database {

  private[this] final case class Config(
    connectionUrl: String,
    username: String,
    password: String,
    schema: String
  )

  private[this] def flywayMigration[F[_]](config: Config)(implicit F: Sync[F]): F[Int] =
    F.delay(
      Flyway
        .configure()
        .schemas(config.schema)
        .defaultSchema(config.schema)
        .dataSource(config.connectionUrl, config.username, config.password)
        .load()
        .migrate()
        .migrationsExecuted
    )

  private[this] def h2Transactor[F[_]: Async: ContextShift](config: Config): Resource[F, H2Transactor[F]] =
    for {
      ec         <- ExecutionContexts.fixedThreadPool[F](32)
      blockingEC <- Blocker[F]
      xa <- H2Transactor.newH2Transactor[F](
        url = config.connectionUrl,
        user = config.username,
        pass = config.password,
        connectEC = ec,
        blocker = blockingEC
      )
    } yield xa

  // http://h2database.com/html/main.html
  // TODO console http://www.h2database.com/html/tutorial.html#using_server
  def initH2[F[_]: Async: ContextShift](implicit log: LogWriter[F]): Resource[F, H2Transactor[F]] = {
    val config = Config("jdbc:h2:mem:example_db;DB_CLOSE_DELAY=-1", "sa", "", "example")

    for {
      _       <- Resource.eval(log.info(s"Init H2 ..."))
      _       <- Resource.eval(log.info(s"config: $config"))
      version <- Resource.eval(flywayMigration[F](config))
      _       <- Resource.eval(log.info(s"migration version: $version"))
      xa      <- h2Transactor[F](config)
    } yield xa
  }
}
