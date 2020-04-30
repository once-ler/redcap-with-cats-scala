package com.eztier.redcap.entity.limsmock.domain
package types

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}
import io.circe.{Decoder, Encoder}
import scala.util.Try
import com.eztier.common.Util._

case class LimsSpecimen
(
  SSTUDYID: Option[String] = None,
  U_MRN: Option[String] = None,
  U_FIRSTNAME: Option[String] = None,
  U_LASTNAME: Option[String] = None,
  BIRTHDATE: Option[LocalDate] = None,
  STUDYLINKID: Option[String] = None,
  USE_STUDYLINKID: Option[Boolean] = None,
  SAMPLEKEY: Option[String] = None,
  SAMPLEVALUE: Option[String] = None,
  SAMPLE_COLLECTION_DATE: Option[LocalDate] = None,
  CREATEDATE: Option[Instant] = None,
  MODIFYDATE: Option[Instant] = None
)

object LimsSpecimen {
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
