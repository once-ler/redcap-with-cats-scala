package com.eztier.redcap.entity.limsmock.domain
package types

import java.time.{Instant, LocalDate}

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
