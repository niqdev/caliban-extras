package com.github.niqdev.caliban

import cats.effect.{ Resource, Sync }
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.foldable._
import cats.syntax.functor._
import cats.syntax.nested._
import cats.syntax.option._
import cats.syntax.traverse._
import com.github.niqdev.caliban.codecs._
import com.github.niqdev.caliban.models._
import com.github.niqdev.caliban.repositories._
import com.github.niqdev.caliban.schemas._

object services {

  /**
    * User service
    */
  sealed abstract class UserService[F[_]: Sync](
    userRepo: UserRepo[F],
    repositoryService: RepositoryService[F]
  ) extends PaginationService[F, User, UserNode[F]] {

    private[this] implicit val nodeSchemaEncoder: SchemaEncoder[User, UserNode[F]] =
      user =>
        (user, repositoryService.findByName, repositoryService.findRepositories(user.id.some))
          .encode[UserNode[F]]

    def findNode: NodeId => F[Option[UserNode[F]]] =
      findNodeById[UserId](userRepo.findById)

    def findByName: UserArg => F[Option[UserNode[F]]] =
      userArg => userRepo.findByName(userArg.name).nested.map(nodeSchemaEncoder.from).value

    def findUsers: UsersArg => F[Connection[F, UserNode[F]]] =
      findConnection(userRepo.find, userRepo.count)
  }

  /**
    * Repository service
    */
  sealed abstract class RepositoryService[F[_]: Sync](
    repositoryRepo: RepositoryRepo[F],
    issueService: IssueService[F]
  ) extends PaginationService[F, Repository, RepositoryNode[F]] {

    private[this] implicit val nodeSchemaEncoder: SchemaEncoder[Repository, RepositoryNode[F]] =
      repository =>
        (repository, issueService.findByNumber, issueService.findIssues(repository.id.some))
          .encode[RepositoryNode[F]]

    def findNode: NodeId => F[Option[RepositoryNode[F]]] =
      findNodeById[RepositoryId](repositoryRepo.findById)

    def findByName: RepositoryArg => F[Option[RepositoryNode[F]]] =
      repositoryArg => repositoryRepo.findByName(repositoryArg.name).nested.map(nodeSchemaEncoder.from).value

    def findRepositories(
      maybeUserId: Option[UserId]
    ): RepositoriesArg => F[Connection[F, RepositoryNode[F]]] = {
      val findItems: (Limit, Option[RowNumber]) => F[List[(Repository, RowNumber)]] =
        maybeUserId.fold(repositoryRepo.find)(repositoryRepo.findByUserId)
      val countItems: F[Count] =
        maybeUserId.fold(repositoryRepo.count)(repositoryRepo.countByUserId)

      findConnection(findItems, countItems)
    }
  }

  /**
    * Issue service
    */
  sealed abstract class IssueService[F[_]: Sync](
    issueRepo: IssueRepo[F]
  ) extends PaginationService[F, Issue, IssueNode[F]] {

    def findNode: NodeId => F[Option[IssueNode[F]]] =
      findNodeById[IssueId](issueRepo.findById)

    def findByNumber: IssueArg => F[Option[IssueNode[F]]] =
      issuerArg => issueRepo.findByNumber(issuerArg.number).nested.map(_.encode[IssueNode[F]]).value

    def findIssues(
      maybeRepositoryId: Option[RepositoryId]
    ): IssuesArg => F[Connection[F, IssueNode[F]]] = {
      val findItems: (Limit, Option[RowNumber]) => F[List[(Issue, RowNumber)]] =
        maybeRepositoryId.fold(issueRepo.find)(issueRepo.findByRepositoryId)
      val countItems: F[Count] =
        maybeRepositoryId.fold(issueRepo.count)(issueRepo.countByRepositoryId)

      findConnection(findItems, countItems)
    }
  }

  /**
    * Pagination service
    */
  sealed abstract class PaginationService[F[_], M, N <: Node[F]](implicit F: Sync[F]) {

    // I: schema id
    protected def findNodeById[I](findM: I => F[Option[M]])(
      implicit idSchemaDecoder: SchemaDecoder[NodeId, I],
      nodeSchemaEncoder: SchemaEncoder[M, N]
    ): NodeId => F[Option[N]] =
      id =>
        F.fromEither(id.decode[I])
          .flatMap(findM)
          .nested
          .map(_.encode[N])
          .value

    protected def findConnection(
      findItems: (Limit, Option[RowNumber]) => F[List[(M, RowNumber)]],
      countItems: => F[Count]
    )(
      implicit nodeSchemaEncoder: SchemaEncoder[M, N]
    ): ForwardPaginationArg => F[Connection[F, N]] =
      paginationArg => {

        def hasSameSize(items: List[(M, RowNumber)], limit: Limit) =
          items.length == limit.value.value

        def dropLastItem(items: List[(M, RowNumber)], limit: Limit) =
          if (hasSameSize(items, limit)) items.dropRight(1) else items

        for {
          limit <- F.fromEither(paginationArg.first.decode[Limit])
          limitPlusOne = Limit.inc(limit)
          nextRowNumber <- F.fromEither(paginationArg.after.decode[Option[RowNumber]])
          // fetch N+1 items to efficiently verify hasNextPage
          itemsPlusOne <- findItems(limitPlusOne, nextRowNumber)
          // items size is always <= N: remove the last item if size is N+1
          items = dropLastItem(itemsPlusOne, limitPlusOne)
          edges <- F.pure(items).nested.map(_.encode[Edge[F, N]]).value
          nodes <- F.pure(items.map(_._1)).nested.map(_.encode[N]).value
          pageInfo <- F.pure {
            PageInfo(
              hasNextPage = hasSameSize(itemsPlusOne, limitPlusOne),
              hasPreviousPage = false, // default false
              startCursor = items.head._2.encode[Cursor],
              endCursor = items.last._2.encode[Cursor]
            )
          }
          totalCount <- countItems.map(_.value)
        } yield Connection(edges, nodes, pageInfo, totalCount)
      }
  }

  /**
    * Node service
    */
  sealed abstract class NodeService[F[_]](
    userService: UserService[F],
    repositoryService: RepositoryService[F],
    issueService: IssueService[F]
  )(implicit F: Sync[F]) {

    // TODO handle specific error + log WARN
    private[this] def ignoreInvalidNode[T <: Node[F]]: PartialFunction[Throwable, Option[T]] = {
      case _: IllegalArgumentException => None
    }

    private[this] def findNode(id: NodeId): F[Option[Node[F]]] =
      for {
        userNode       <- userService.findNode(id).recover(ignoreInvalidNode)
        repositoryNode <- repositoryService.findNode(id).recover(ignoreInvalidNode)
        issueNode      <- issueService.findNode(id).recover(ignoreInvalidNode)
      } yield List(userNode, repositoryNode, issueNode).collectFirstSomeM(List(_)).head

    def findNode: NodeArg => F[Option[Node[F]]] =
      nodeArg => findNode(nodeArg.id)

    def findNodes: NodesArg => F[List[Option[Node[F]]]] =
      nodesArg => F.pure(nodesArg.ids).flatMap(_.traverse(findNode))
  }

  /**
    * Services
    */
  sealed trait Services[F[_]] {
    def nodeService: NodeService[F]
    def userService: UserService[F]
    def repositoryService: RepositoryService[F]
    def issueService: IssueService[F]
  }
  object Services {
    private[this] def apply[F[_]: Sync](repos: Repositories[F]) =
      new Services[F] {
        val issueService: IssueService[F] =
          new IssueService[F](repos.issueRepo) {}
        val repositoryService: RepositoryService[F] =
          new RepositoryService[F](repos.repositoryRepo, issueService) {}
        val userService: UserService[F] =
          new UserService[F](repos.userRepo, repositoryService) {}
        val nodeService: NodeService[F] =
          new NodeService[F](userService, repositoryService, issueService) {}
      }

    def make[F[_]: Sync](repos: Repositories[F]) =
      Resource.liftF(Sync[F].delay(apply[F](repos)))
  }
}
