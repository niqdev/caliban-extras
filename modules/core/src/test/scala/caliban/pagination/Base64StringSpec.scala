package caliban.pagination

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

// TODO prop tests
final class Base64StringSpec extends AnyWordSpecLike with Matchers {

  private[this] val helloBase64 = Base64String.unsafeFrom("aGVsbG8=")

  "Base64String" should {

    "verify valid encode" in {
      Base64String.encode("hello") shouldBe Right(helloBase64)
    }

    "verify invalid encode" in {
      Base64String.encode("") match {
        case Left(error) =>
          error.getMessage should startWith("Predicate failed")
        case _ =>
          fail("unexpected error")
      }
    }

    "verify valid decode" in {
      Base64String.decode(helloBase64) shouldBe Right("hello")
    }

    "verify removePrefix" in {
      Base64String.unsafeRemovePrefixes("yyy:123", "xxx:", "yyy:") shouldBe "123"
    }
  }
}
