package caliban.refined

import zio.test._
import zio.test.environment.TestEnvironment

object RefinedSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("RefinedSpec")(
      test("example") {
        assert("hello")(Assertion.isNonEmptyString)
      }
    )
}
