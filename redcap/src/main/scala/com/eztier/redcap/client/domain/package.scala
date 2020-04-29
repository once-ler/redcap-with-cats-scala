package com.eztier
package redcap.client

import java.time.format.DateTimeFormatter

import io.circe.{Decoder, Encoder}
import java.time.{Instant, LocalDate}

import scala.util.Try
import common.Util._

package object domain {
  private val defaultDateTimeFormatterString = "yyyy-MM-dd HH:mm:ss"
  private val defaultLocalDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  type Metadata = List[Field]

  implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](i => instantToString(i, Some(defaultDateTimeFormatterString)))

  implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emapTry { str =>
    Try(stringToInstant(str, Some(defaultDateTimeFormatterString)))
  }

  implicit val dateEncoder = Encoder.encodeString.contramap[LocalDate](_.format(defaultLocalDateFormatter))
  implicit val dateDecoder = Decoder.decodeString.emapTry[LocalDate](str => {
    Try(LocalDate.parse(str, defaultLocalDateFormatter))
  })
}
