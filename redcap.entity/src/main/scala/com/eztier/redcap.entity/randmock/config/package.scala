package com.eztier
package redcap.entity.randmock

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

package object config {
  implicit val appDecoder: Decoder[AppConfig] = deriveDecoder
  implicit val dbconnDec: Decoder[DatabaseConnectionsConfig] = deriveDecoder
  implicit val dbInstanceDec: Decoder[DatabaseInstanceConfig] = deriveDecoder
  implicit val dbDec: Decoder[DatabaseConfig] = deriveDecoder
  implicit val httpInstanceDec: Decoder[HttpInstanceConfig] = deriveDecoder
  implicit val httpDec: Decoder[HttpConfig] = deriveDecoder

  implicit val appEncoder: Encoder[AppConfig] = deriveEncoder
  implicit val dbconnEnc: Encoder[DatabaseConnectionsConfig] = deriveEncoder
  implicit val dbInstanceEnc: Encoder[DatabaseInstanceConfig] = deriveEncoder
  implicit val dbEnc: Encoder[DatabaseConfig] = deriveEncoder
  implicit val httpInstanceEnc: Encoder[HttpInstanceConfig] = deriveEncoder
  implicit val httpEnc: Encoder[HttpConfig] = deriveEncoder
}
