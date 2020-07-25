package com.github.niqdev.caliban

import caliban.pagination.arguments._
import caliban.pagination.schemas._
import caliban.{ GraphQL, RootResolver }
import cats.effect.Effect
import com.github.niqdev.caliban.arguments._
import com.github.niqdev.caliban.schema._
import com.github.niqdev.caliban.services._

object resolvers {

  /**
    * Node roots
    *
    * TODO move in caliban.pagination.resolvers
    */
  final case class NodeQueries[F[_]](
    node: NodeArg => F[Option[Node[F]]],
    nodes: NodesArg => F[List[Option[Node[F]]]]
  )
  object NodeQueries {
    private[this] def resolver[F[_]: Effect](services: Services[F]): NodeQueries[F] =
      NodeQueries(
        node = nodeArg => services.nodeService.findNode(nodeArg.id),
        nodes = nodesArg => services.nodeService.findNodes(nodesArg.ids)
      )

    def api[F[_]: Effect](services: Services[F]): GraphQL[Any] =
      GraphQL.graphQL(RootResolver(resolver[F](services)))
  }

  /**
    * GitHub roots
    */
  final case class GitHubQueries[F[_]](
    user: UserArg => F[Option[UserNode[F]]],
    repository: RepositoryArg => F[Option[RepositoryNode[F]]],
    repositories: RepositoriesArg => F[RepositoryConnection[F]]
  )
  object GitHubQueries {
    private[this] def resolver[F[_]: Effect](services: Services[F]): GitHubQueries[F] =
      GitHubQueries(
        user = userArg => services.userService.findByName(userArg.name),
        repository = repositoryArg => services.repositoryService.findByName(repositoryArg.name),
        repositories = services.repositoryService.connection(None)
      )

    def api[F[_]: Effect](services: Services[F]): GraphQL[Any] =
      GraphQL.graphQL(RootResolver(resolver[F](services)))
  }

  // TODO log errors: mapError or Wrapper
  def api[F[_]: Effect](services: Services[F]): GraphQL[Any] =
    NodeQueries.api[F](services) |+|
      GitHubQueries.api[F](services)
}
