package com.eztier.redcap.entity.randmock.domain
package types

import java.time.{Instant, LocalDate}
import java.time.format.DateTimeFormatter

import io.circe.{Decoder, Encoder, derivation}
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import scala.util.Try
import com.eztier.common.Util.{instantToString, stringToInstant}

case class RcLocalRandomization
(
  SubjectId: Option[String] = None,
  Eligible: Option[String] = None,
  Randodat: Option[LocalDate] = None,
  RandoAss: Option[String] = None
)

object RcLocalRandomization {
  implicit val encoder: Encoder[RcLocalRandomization] = deriveEncoder(derivation.renaming.snakeCase, None)
  implicit val decoder: Decoder[RcLocalRandomization] = deriveDecoder(derivation.renaming.snakeCase, true, None)

  private val defaultDateTimeFormatterString = "yyyy-MM-dd HH:mm:ss"
  private val defaultLocalDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](i => instantToString(i, Some(defaultDateTimeFormatterString)))
  implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emapTry { str =>
    Try(stringToInstant(str, Some(defaultDateTimeFormatterString)))
  }

  implicit val dateEncoder: Encoder[LocalDate] = Encoder.encodeString.contramap[LocalDate](_.format(defaultLocalDateFormatter))
  implicit val dateDecoder: Decoder[LocalDate] = Decoder.decodeString.emapTry[LocalDate](str => {
    Try(LocalDate.parse(str, defaultLocalDateFormatter))
  })
}
