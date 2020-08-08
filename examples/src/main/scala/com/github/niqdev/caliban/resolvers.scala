package com.github.niqdev.caliban

import caliban.{ GraphQL, RootResolver }
import cats.effect.{ Effect, Resource, Sync }
import com.github.niqdev.caliban.schemas._
import com.github.niqdev.caliban.services._

object resolvers {
  import caliban.interop.cats.implicits._
  import caliban.filter.schemas._
  import caliban.refined._

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
    // TODO log errors: mapError or Wrapper
    private[this] def api[F[_]: Effect](services: Services[F]): GraphQL[Any] =
      NodeRootResolver.api[F](services) |+|
        GitHubRootResolver.api[F](services)

    def make[F[_]: Effect](services: Services[F]) =
      Resource.liftF(Sync[F].pure(api[F](services)))
  }
}
