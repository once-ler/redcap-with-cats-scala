package com.eztier.redcap.client
package domain

case class Project
(
  ProjectId: Option[Long] = None,
  ProjectTitle: Option[String] = None,
  CreationTime: Option[Instant] = None,
  ProductionTime: Option[String] = None,
  InProduction: Option[Byte] = None,
  ProjectLanguage: Option[String] = Some("English"),
  Purpose: Option[Int] = None,
  PurposeOther: Option[String] = None,
  ProjectNotes: Option[String] = None,
  CustomRecordLabel: Option[String] = None,
  SecondaryUniqueField: Option[String] = None,
  IsLongitudinal: Option[Byte] = None,
  SurveysEnabled: Option[Byte] = None,
  SchedulingEnabled: Option[Byte] = None,
  RecordAutonumberingEnabled: Option[Byte] = None,
  RandomizationEnabled: Option[Byte] = None,
  DdpEnabled: Option[Byte] = None,
  ProjectIrbNumber: Option[String] = None,
  ProjectGrantNumber: Option[String] = None,
  ProjectPiFirstname: Option[String] = None,
  ProjectPiLastname: Option[String] = None,
  DisplayTodayNowButton: Option[Byte] = None,
  HasRepeatingInstrumentsOrEvents: Option[Byte] = None
)
