package com.github.niqdev.caliban

import cats.effect.{ Resource, Sync }
import com.github.niqdev.caliban.models._
import doobie.syntax.all._
import doobie.util.fragment.Fragment
import doobie.util.meta.Meta
import doobie.util.transactor.Transactor
import eu.timepit.refined.types.numeric.{ NonNegInt, NonNegLong, PosInt, PosLong }
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.Coercible
import io.estatico.newtype.macros.newtype

object repositories {

  import doobie.implicits.legacy.instant.JavaTimeInstantMeta
  import doobie.h2.implicits.UuidType
  import doobie.refined.implicits.refinedMeta

  // enable default logging
  private[this] implicit val logHandler =
    doobie.util.log.LogHandler.jdkLogHandler

  // newtype meta
  private[this] implicit def coercibleMeta[R, N](
    implicit ev: Coercible[Meta[R], Meta[N]],
    R: Meta[R]
  ): Meta[N] = ev(R)

  @newtype case class RowNumber(value: PosLong)
  @newtype case class Count(value: NonNegLong)
  @newtype case class Limit(value: NonNegInt)
  object Limit {
    def inc(limit: Limit): Limit =
      Limit(NonNegInt.unsafeFrom(limit.value.value + 1))
  }

  /**
    * User repository
    */
  sealed abstract class UserRepo[F[_]: Sync](xa: Transactor[F]) {

    def findById(id: UserId): F[Option[User]] =
      UserRepo.queries.findById(id).query[User].option.transact(xa)

    def findByName(name: NonEmptyString): F[Option[User]] =
      UserRepo.queries.findByName(name).query[User].option.transact(xa)

    val find: (Limit, Option[RowNumber]) => F[List[(User, RowNumber)]] =
      (limit, nextRowNumber) =>
        UserRepo
          .queries
          .find(limit, nextRowNumber)
          .query[(User, RowNumber)]
          .to[List]
          .transact(xa)

    def count: F[Count] =
      UserRepo.queries.count.query[Count].unique.transact(xa)
  }
  object UserRepo {
    def apply[F[_]: Sync](xa: Transactor[F]): UserRepo[F] =
      new UserRepo[F](xa) {}

    private[UserRepo] object queries extends CommonQueries {
      private[this] val schemaName = "example"
      private[this] val tableName  = "user"

      override def fromTable: Fragment =
        Fragment.const(s" FROM $schemaName.$tableName ")

      override def columns: Fragment =
        fr" id, name, created_at, updated_at "

      override def orderBy: Fragment =
        fr" ORDER BY updated_at "

      lazy val findById: UserId => Fragment =
        id => findAll(where = fr" WHERE id = $id ")

      lazy val findByName: NonEmptyString => Fragment =
        name => findAll(where = fr" WHERE name = $name ")
    }
  }

  /**
    * Repository repository
    */
  sealed abstract class RepositoryRepo[F[_]: Sync](xa: Transactor[F]) {

    def findById(id: RepositoryId): F[Option[Repository]] =
      RepositoryRepo.queries.findById(id).query[Repository].option.transact(xa)

    def findByName(name: NonEmptyString): F[Option[Repository]] =
      RepositoryRepo.queries.findByName(name).query[Repository].option.transact(xa)

    val find: (Limit, Option[RowNumber]) => F[List[(Repository, RowNumber)]] =
      (limit, nextRowNumber) =>
        RepositoryRepo
          .queries
          .find(limit, nextRowNumber)
          .query[(Repository, RowNumber)]
          .to[List]
          .transact(xa)

    def findByUserId(userId: UserId): (Limit, Option[RowNumber]) => F[List[(Repository, RowNumber)]] =
      (limit, nextRowNumber) =>
        RepositoryRepo
          .queries
          .findByUserId(limit, nextRowNumber)(userId)
          .query[(Repository, RowNumber)]
          .to[List]
          .transact(xa)

    def count: F[Count] =
      RepositoryRepo.queries.count.query[Count].unique.transact(xa)

    def countByUserId(userId: UserId): F[Count] =
      RepositoryRepo.queries.countByUserId(userId).query[Count].unique.transact(xa)
  }
  object RepositoryRepo {
    def apply[F[_]: Sync](xa: Transactor[F]): RepositoryRepo[F] =
      new RepositoryRepo[F](xa) {}

    private[RepositoryRepo] object queries extends CommonQueries {
      private[this] val schemaName = "example"
      private[this] val tableName  = "repository"

      override def fromTable: Fragment =
        Fragment.const(s" FROM $schemaName.$tableName ")

      override def columns: Fragment =
        fr" id, user_id, name, url, is_fork, created_at, updated_at "

      override def orderBy: Fragment =
        fr" ORDER BY updated_at "

      lazy val findById: RepositoryId => Fragment =
        id => findAll(where = fr" WHERE id = $id ")

      lazy val findByName: NonEmptyString => Fragment =
        name => findAll(where = fr" WHERE name = $name ")

      // TODO orderBy
      def findByUserId(limit: Limit, nextRowNumber: Option[RowNumber]): UserId => Fragment =
        userId => find(Some(fr" WHERE user_id = $userId "), None, limit, nextRowNumber)

      lazy val countByUserId: UserId => Fragment =
        userId => fr"SELECT COUNT(*)" ++ fromTable ++ fr"WHERE user_id = $userId"
    }
  }

  /**
    * Issue repository
    */
  sealed abstract class IssueRepo[F[_]: Sync](xa: Transactor[F]) {

    def findById(id: IssueId): F[Option[Issue]] =
      IssueRepo.queries.findById(id).query[Issue].option.transact(xa)

    def findByNumber(number: PosInt): F[Option[Issue]] =
      IssueRepo.queries.findByNumber(number).query[Issue].option.transact(xa)

    val find: (Limit, Option[RowNumber]) => F[List[(Issue, RowNumber)]] =
      (limit, nextRowNumber) =>
        IssueRepo
          .queries
          .find(limit, nextRowNumber)
          .query[(Issue, RowNumber)]
          .to[List]
          .transact(xa)

    def findByRepositoryId(
      repositoryId: RepositoryId
    ): (Limit, Option[RowNumber]) => F[List[(Issue, RowNumber)]] =
      (limit, nextRowNumber) =>
        IssueRepo
          .queries
          .findByRepositoryId(limit, nextRowNumber)(repositoryId)
          .query[(Issue, RowNumber)]
          .to[List]
          .transact(xa)

    def count: F[Count] =
      IssueRepo.queries.count.query[Count].unique.transact(xa)

    def countByRepositoryId(repositoryId: RepositoryId): F[Count] =
      IssueRepo.queries.countByRepositoryId(repositoryId).query[Count].unique.transact(xa)
  }
  object IssueRepo {
    def apply[F[_]: Sync](xa: Transactor[F]): IssueRepo[F] =
      new IssueRepo[F](xa) {}

    private[IssueRepo] object queries extends CommonQueries {
      private[this] val schemaName = "example"
      private[this] val tableName  = "issue"

      override def fromTable: Fragment =
        Fragment.const(s" FROM $schemaName.$tableName ")

      override def columns: Fragment =
        fr" id, repository_id, number, status, title, body, created_at, updated_at "

      override def orderBy: Fragment =
        fr" ORDER BY updated_at "

      lazy val findById: IssueId => Fragment =
        id => findAll(where = fr" WHERE id = $id ")

      lazy val findByNumber: PosInt => Fragment =
        number => findAll(where = fr" WHERE number = $number ")

      // TODO orderBy
      def findByRepositoryId(limit: Limit, nextRowNumber: Option[RowNumber]): RepositoryId => Fragment =
        repositoryId => find(Some(fr" WHERE repository_id = $repositoryId "), None, limit, nextRowNumber)

      lazy val countByRepositoryId: RepositoryId => Fragment =
        repositoryId => fr"SELECT COUNT(*)" ++ fromTable ++ fr"WHERE repository_id = $repositoryId"
    }
  }

  trait CommonQueries {

    def fromTable: Fragment
    def columns: Fragment
    def orderBy: Fragment

    private[this] val nop = fr""

    //@scala.annotation.nowarn due to private[this]
    def select(
      columns: Fragment,
      fromTable: Fragment,
      maybeWhere: Option[Fragment] = None,
      maybeExtraColumns: Option[Fragment] = None,
      maybeOrderBy: Option[Fragment] = None,
      maybeLimit: Option[Fragment] = None
    ): Fragment =
      fr"SELECT " ++ columns ++ maybeExtraColumns.getOrElse(nop) ++
        fromTable ++
        maybeWhere.getOrElse(nop) ++
        maybeOrderBy.getOrElse(nop) ++
        maybeLimit.getOrElse(nop)

    private[this] def selectRows(
      columns: Fragment,
      fromTable: Fragment,
      maybeWhere: Option[Fragment],
      orderBy: Fragment,
      limit: Fragment,
      nextRowNumber: Option[RowNumber]
    ): Fragment = {

      val rowNumberColumn = fr", ROW_NUMBER() OVER (" ++ orderBy ++ fr") AS row_number "
      val selectLimit: Fragment =
        select(columns, fromTable, maybeWhere, Some(rowNumberColumn), Some(orderBy), Some(limit))
      val selectLimitAfterRowNumber: RowNumber => Fragment = rowNumber =>
        fr"SELECT * FROM (" ++
          select(columns, fromTable, maybeWhere, Some(rowNumberColumn), Some(orderBy), None) ++
          fr") t WHERE t.row_number > $rowNumber" ++ limit

      nextRowNumber.fold(selectLimit)(selectLimitAfterRowNumber)
    }

    def findAll(
      where: Fragment,
      maybeOrderBy: Option[Fragment] = None
    ): Fragment =
      select(
        columns,
        fromTable,
        Some(where),
        None,
        maybeOrderBy.orElse(Some(orderBy)),
        None
      )

    def find(
      maybeWhere: Option[Fragment],
      maybeOrderBy: Option[Fragment],
      limit: Limit,
      nextRowNumber: Option[RowNumber]
    ): Fragment =
      selectRows(
        columns,
        fromTable,
        maybeWhere,
        maybeOrderBy.getOrElse(orderBy),
        fr" LIMIT $limit ",
        nextRowNumber
      )

    // TODO orderBy
    def find(limit: Limit, nextRowNumber: Option[RowNumber]): Fragment =
      find(None, None, limit, nextRowNumber)

    def count: Fragment =
      fr"SELECT COUNT(*)" ++ fromTable
  }

  /**
    * Repositories
    */
  sealed trait Repositories[F[_]] {
    def userRepo: UserRepo[F]
    def repositoryRepo: RepositoryRepo[F]
    def issueRepo: IssueRepo[F]
  }
  object Repositories {
    private[this] def apply[F[_]: Sync](xa: Transactor[F]): Repositories[F] =
      new Repositories[F] {
        val userRepo: UserRepo[F]             = UserRepo[F](xa)
        val repositoryRepo: RepositoryRepo[F] = RepositoryRepo[F](xa)
        val issueRepo: IssueRepo[F]           = IssueRepo[F](xa)
      }

    def make[F[_]: Sync](xa: Transactor[F]): Resource[F, Repositories[F]] =
      Resource.eval(Sync[F].delay(apply[F](xa)))
  }
}
