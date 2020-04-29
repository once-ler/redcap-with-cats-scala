package com.eztier.redcap.entity.limsmock.domain
package types

import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, derivation}
import java.time.{Instant, LocalDate}

case class RcSpecimen
(
  RedcapRepeatInstrument: Option[String] = Some("research_specimens"),
  RecordId: Option[String] = None,
  RedcapRepeatInstance: Option[Int] = None,
  SpecDate: Option[LocalDate] = None,
  SpecLvParticipantId: Option[String] = None,
  SpecEvent: Option[String] = None,
  SpecSpr: Option[String] = None,
  SpecSampleParentId: Option[String] = None,
  SpecTissueStatus: Option[String] = None,
  SpecQuantity: Option[String] = None,
  SpecUnit: Option[String] = None,
  SpecContainerType: Option[String] = None,
  SpecStorageStatus: Option[String] = None,
  SpecLocation: Option[String] = None,
  SpecModifyDate: Option[Instant] = None
)

object RcSpecimen {
  implicit val encoder: Encoder[RcSpecimen] = deriveEncoder(derivation.renaming.snakeCase, None)
  implicit val decoder: Decoder[RcSpecimen] = deriveDecoder(derivation.renaming.snakeCase, true, None)
}