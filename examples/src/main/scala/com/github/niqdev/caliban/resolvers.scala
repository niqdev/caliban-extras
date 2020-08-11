package com.github.niqdev.caliban

import caliban.wrappers.Wrapper.OverallWrapper
import caliban.{ GraphQL, RootResolver }
import cats.effect.{ Effect, Resource, Sync }
import com.github.niqdev.caliban.schemas._
import com.github.niqdev.caliban.services._
import log.effect.LogWriter

object resolvers {
  import caliban.interop.cats.implicits._
  import caliban.refined._

  /**
    * Node resolver
    */
  private[this] object NodeQuery {
    def resolver[F[_]: Effect](services: Services[F]): NodeRoot[F] =
      NodeRoot(
        node = services.nodeService.findNode,
        nodes = services.nodeService.findNodes
      )
  }
  object NodeRootResolver {
    def api[F[_]: Effect](services: Services[F]): GraphQL[Any] =
      GraphQL.graphQL(RootResolver(NodeQuery.resolver[F](services)))
  }

  /**
    * GitHub resolver
    */
  private[this] object GitHubQuery {
    def resolver[F[_]: Effect](services: Services[F]): GitHubRoot[F] =
      GitHubRoot(
        user = services.userService.findByName,
        users = services.userService.findUsers,
        repository = services.repositoryService.findByName,
        repositories = services.repositoryService.findRepositories(None),
        issue = services.issueService.findByNumber,
        issues = services.issueService.findIssues(None)
      )
  }
  object GitHubRootResolver {
    def api[F[_]: Effect](services: Services[F]): GraphQL[Any] =
      GraphQL.graphQL(RootResolver(GitHubQuery.resolver[F](services)))
  }

  /**
    * Resolvers
    */
  object Resolvers {
    private[this] def logWrapper[F[_]](implicit logger: LogWriter[F]): OverallWrapper[Any] =
      OverallWrapper { process => request =>
        process(request)
          .map { response =>
            if (response.errors.nonEmpty)
              logger.error(s"""
                   |request: $request
                   |errors: ${response.errors.mkString("\n")}
                   |""".stripMargin)
            else
              logger.debug(s"""
                   |request: $request
                   |response: $response
                   |""".stripMargin)
            response
          }
      }

    private[this] def api[F[_]: Effect: LogWriter](services: Services[F]): GraphQL[Any] =
      NodeRootResolver.api[F](services) |+|
        GitHubRootResolver.api[F](services) @@
          logWrapper

    def make[F[_]: Effect: LogWriter](services: Services[F]) =
      Resource.liftF(Sync[F].delay(api[F](services)))
  }
}
