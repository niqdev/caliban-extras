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
  final object Base64String extends RefinedTypeOps[Base64String, String] {

    /**
      * Encodes from a plain String
      */
    lazy val encode: String => Either[String, Base64String] =
      value =>
        Either
          .catchNonFatal(Base64.getEncoder.encodeToString(value.getBytes(StandardCharsets.UTF_8)))
          .leftMap(_.getMessage)
          .flatMap(Base64String.from)

    /**
      * Decodes to a plain String
      */
    lazy val decode: Base64String => Either[String, String] =
      base64String =>
        Either
          .catchNonFatal(new String(Base64.getDecoder.decode(base64String.value), StandardCharsets.UTF_8))
          .leftMap(_.getMessage)

    def removePrefix(value: String, prefixes: String*): String =
      prefixes.foldLeft(value)((v, prefix) => v.replace(prefix, ""))
  }
}
