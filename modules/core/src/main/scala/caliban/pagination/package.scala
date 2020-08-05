package caliban

import java.nio.charset.StandardCharsets
import java.util.Base64

import cats.syntax.either._
import eu.timepit.refined.W
import eu.timepit.refined.api.{ Refined, RefinedTypeOps }
import eu.timepit.refined.string.MatchesRegex

package object pagination {

  // https://stackoverflow.com/questions/475074/regex-to-parse-or-validate-base64-data
  final val Base64Regex = W(
    """^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4})$"""
  )
  final type Base64String = String Refined MatchesRegex[Base64Regex.T]
  // TODO move prefix in Schema/ArgBuilder
  final object Base64String extends RefinedTypeOps[Base64String, String] {

    /**
      * Encodes from plain String
      */
    lazy val encode: String => Either[Throwable, Base64String] =
      value =>
        Either
          .catchNonFatal(Base64.getEncoder.encodeToString(value.getBytes(StandardCharsets.UTF_8)))
          .flatMap(base64String => Base64String.from(base64String).leftMap(new IllegalArgumentException(_)))

    /**
      * Encodes from plain String with prefix
      */
    def encodeWithPrefix(value: String, prefix: String): Either[Throwable, Base64String] =
      encode(s"$prefix$value")

    /**
      * Decodes to plain String
      */
    lazy val decode: Base64String => Either[Throwable, String] =
      base64String =>
        Either
          .catchNonFatal(new String(Base64.getDecoder.decode(base64String.value), StandardCharsets.UTF_8))

    private[pagination] def unsafeRemovePrefixes(value: String, prefixes: String*): String =
      prefixes.foldLeft(value)((v, prefix) => v.replace(prefix, ""))

    private[pagination] def removePrefix(value: String, prefix: String): Either[Throwable, String] = {
      val errorMessage = s"invalid prefix: expected to start with [$prefix] but found [$value]"
      Either
        .cond(
          value.startsWith(prefix),
          unsafeRemovePrefixes(value, prefix),
          new IllegalArgumentException(errorMessage)
        )
    }

    /**
      * Decodes to plain String and strip prefix
      */
    def decodeWithoutPrefix(base64String: Base64String, prefix: String): Either[Throwable, String] =
      decode(base64String).flatMap(value => removePrefix(value, prefix))
  }
}
