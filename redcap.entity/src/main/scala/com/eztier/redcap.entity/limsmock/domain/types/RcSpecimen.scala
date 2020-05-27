package com.eztier.redcap.entity.limsmock.domain
package types

import io.circe.{Decoder, Encoder, derivation}
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}
import scala.util.Try

import com.eztier.common.Util._

case class RcSpecimen
(
  RedcapRepeatInstrument: Option[String] = Some("research_specimens"),
  RecordId: Option[String] = None,
  RedcapRepeatInstance: Option[Int] = None,
  SpecLvParticipantId: Option[String] = None,
  SpecEvent: Option[String] = None,
  SpecDate: Option[LocalDate] = None,
  SpecSampleType: Option[String] = None,
  SpecSampleKey: Option[String] = None,
  SpecTissueStatus: Option[String] = None,
  SpecAnatomicSite: Option[String] = None,
  SpecSurgicalPathNbr: Option[String] = None,
  SpecQuantity: Option[String] = None,
  SpecUnit: Option[String] = None,
  SpecStorageStatus: Option[String] = None,
  SpecLocation: Option[String] = None,
  SpecModifyDate: Option[Instant] = None
)

object RcSpecimen {
  implicit val encoder: Encoder[RcSpecimen] = deriveEncoder(derivation.renaming.snakeCase, None)
  implicit val decoder: Decoder[RcSpecimen] = deriveDecoder(derivation.renaming.snakeCase, true, None)

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