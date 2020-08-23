package caliban.refined

import caliban.Macros.gqldoc
import caliban.{ GraphQL, RootResolver }
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object RefinedSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("RefinedSpec")(
      testM("verify refined Schema") {
        final case class User(name: String)
        final case class Query(user: User)

        val resolver = Query(User("myName"))
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
        final case class User(id: Int, name: String)
        final case class UserArg(id: Int)
        final case class Query(user: UserArg => User)

        val resolver = Query(arg => User(arg.id, "myName"))
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
