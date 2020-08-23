package caliban.refined

import caliban.Macros.gqldoc
import caliban.{ GraphQL, RootResolver }
import eu.timepit.refined.types.all.{ NonEmptyString, PosInt }
import io.estatico.newtype.macros.newtype
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object RefinedSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("RefinedSpec")(
      testM("verify refined Schema") {
        final case class User(name: NonEmptyString)
        final case class Query(user: User)

        val resolver = Query(User(NonEmptyString.unsafeFrom("myName")))
        val api      = GraphQL.graphQL(RootResolver(resolver))
        val query    = gqldoc("""
          query {
            user {
              name
            }
          }
          """)

        assertM(api.interpreter.flatMap(_.execute(query)).map(_.data.toString))(
          equalTo("""{"user":{"name":"myName"}}""")
        )
      },
      testM("verify refined newtype Schema") {
        import eu.timepit.refined.auto.autoRefineV

        @newtype case class Name(private val string: NonEmptyString)
        final case class User(name: Name)
        final case class Query(user: User)

        val resolver = Query(User(Name("myName")))
        val api      = GraphQL.graphQL(RootResolver(resolver))
        val query    = gqldoc("""
          query {
            user {
              name
            }
          }
          """)

        assertM(api.interpreter.flatMap(_.execute(query)).map(_.data.toString))(
          equalTo("""{"user":{"name":"myName"}}""")
        )
      },
      testM("verify refined ArgBuilder") {
        final case class User(id: PosInt, name: NonEmptyString)
        final case class UserArg(id: PosInt)
        final case class Query(user: UserArg => User)

        val resolver = Query(arg => User(arg.id, NonEmptyString.unsafeFrom("myName")))
        val api      = GraphQL.graphQL(RootResolver(resolver))
        val query    = gqldoc("""
          query {
            user(id: 1) {
              id
              name
            }
          }
          """)

        assertM(api.interpreter.flatMap(_.execute(query)).map(_.data.toString))(
          equalTo("""{"user":{"id":1,"name":"myName"}}""")
        )
      },
      testM("verify refined newtype ArgBuilder") {
        import eu.timepit.refined.auto.autoRefineV

        @newtype case class Id(private val int: PosInt)
        @newtype case class Name(private val string: NonEmptyString)
        final case class User(id: Id, name: Name)
        final case class UserArg(id: Id)
        final case class Query(user: UserArg => User)

        val resolver = Query(arg => User(arg.id, Name("myName")))
        val api      = GraphQL.graphQL(RootResolver(resolver))
        val query    = gqldoc("""
          query {
            user(id: 1) {
              id
              name
            }
          }
          """)

        assertM(api.interpreter.flatMap(_.execute(query)).map(_.data.toString))(
          equalTo("""{"user":{"id":1,"name":"myName"}}""")
        )
      }
    )
}
