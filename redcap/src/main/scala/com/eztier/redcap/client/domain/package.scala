package com.eztier
package redcap.client

import java.time.Instant
import java.time.format.DateTimeFormatter

import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

package object domain {
  private val defaultDateTimeFormatterString = "yyyy-MM-dd HH:mm:ss"

  type Metadata = List[Field]

  /*
  implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](i => instantToString(i, Some(defaultDateTimeFormatterString)))

  implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emapTry { str =>
    Try(stringToInstant(str, Some(defaultDateTimeFormatterString)))
  }
  */
}
