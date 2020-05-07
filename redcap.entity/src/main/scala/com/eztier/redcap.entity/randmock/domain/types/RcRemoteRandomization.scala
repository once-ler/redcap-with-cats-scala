package com.eztier.redcap.entity.randmock.domain.types

import java.time.{Instant, LocalDate}
import java.time.format.DateTimeFormatter

import com.eztier.common.Util.{instantToString, stringToInstant}
import io.circe.{Decoder, Encoder, derivation}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

import scala.util.Try

case class RcRemoteRandomization
(
  RecordId: Option[String] = None,
  Rmyn: Option[String] = None,
  Rmrdate: Option[LocalDate] = None,
  Ragroup: Option[String] = None
)

object RcRemoteRandomization {
  implicit val encoder: Encoder[RcRemoteRandomization] = deriveEncoder(derivation.renaming.snakeCase, None)
  implicit val decoder: Decoder[RcRemoteRandomization] = deriveDecoder(derivation.renaming.snakeCase, true, None)

  private val defaultDateTimeFormatterString = "yyyy-MM-dd HH:mm:ss"
  private val defaultLocalDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](i => instantToString(i, Some(defaultDateTimeFormatterString)))
  implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emapTry { str =>
    Try(stringToInstant(str, Some(defaultDateTimeFormatterString)))
  }

  implicit val dateEncoder: Encoder[LocalDate] = Encoder.encodeString.contramap[LocalDate](_.format(defaultLocalDateFormatter))
  implicit val dateDecoder: Decoder[LocalDate] = Decoder.decodeString.emapTry[LocalDate](str => {
    val nstr = if (str.isEmpty) "1900-01-01" else str
    Try(LocalDate.parse(nstr, defaultLocalDateFormatter))
  })
}