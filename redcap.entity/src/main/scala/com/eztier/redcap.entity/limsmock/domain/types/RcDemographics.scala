package com.eztier.redcap.entity.limsmock.domain
package types

import io.circe.{Decoder, Encoder, derivation}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

case class RcDemographics
(
  RecordId: Option[String] = None,
  Mrn: Option[String] = None,
  SpcId: Option[String] = None,
  LbvId: Option[String] = None
)

object RcDemographics {
  implicit val encoder: Encoder[RcDemographics] = deriveEncoder(derivation.renaming.snakeCase, None)
  implicit val decoder: Decoder[RcDemographics] = deriveDecoder(derivation.renaming.snakeCase, true, None)
}
