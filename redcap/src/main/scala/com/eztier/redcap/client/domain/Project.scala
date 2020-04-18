package com.eztier
package redcap.client
package domain

import io.circe._
import io.circe.generic.extras._
import io.circe.syntax._
import io.circe.parser._

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.generic.extras._
import java.time.Instant

// @ConfiguredJsonCodec
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

object Project {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
  implicit val snakyEncoder: Encoder[Project] = deriveEncoder
  implicit val snakyDecoder: Decoder[Project] = deriveDecoder
}
