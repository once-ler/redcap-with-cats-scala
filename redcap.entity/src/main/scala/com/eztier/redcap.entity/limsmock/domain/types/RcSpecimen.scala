package com.eztier.redcap.entity.limsmock.domain
package types

import java.time.Instant

case class RcSpecimen
(
  RedcapRepeatInstrument: Option[String] = Some("research_specimens"),
  RecordId: Option[String] = None,
  RedcapRepeatInstance: Option[Int] = None,
  SpecDate: Option[Instant] = None,
  SpecLvParticipantId: Option[String] = None,
  SpecEvent: Option[String] = None,
  SpecSpr: Option[String] = None,
  SpecSampleParentId: Option[String] = None,
  SpecTissueStatus: Option[String] = None,
  SpecQuantity: Option[String] = None,
  SpecUnit: Option[String] = None,
  SpecContainerType: Option[String] = None,
  SpecStorageStatus: Option[String] = None,
  SpecLocation: Option[String] = None
)
