package com.eztier
package redcap.client
package test

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}

import cats.implicits._
import cats.data.Chain
import cats.effect.{IO, Sync}
import fs2.{Pipe, Stream}
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json, derivation}
import io.circe.syntax._
import org.http4s.{Header, Headers}
import org.specs2.mutable._

import scala.util.Try

// import io.circe.generic.semiauto._
import domain._

import com.eztier.common.Util._

object TestFixtures {
  
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

  val odmFilePath = System.getProperty("user.dir") + "/internal/test-odm-template.xml"

  val projectId = "20-XXXXXX".some
  val proj = Project(
    ProjectTitle = projectId,
    Purpose = Some(4),
    ProjectNotes = projectId,
    ProjectIrbNumber = projectId
  )

  val patid = "ABCDEFG"
  val record: String => Chain[(String, String)] =
    forms => Chain(
      "content" -> "record",
      "forms" -> forms,
      "records" -> patid
      // "filterLogic" -> s"[record_id] = '$patid'"
    )

  case class RcDemographics
  (
    recordId: Option[String] = None,
    Mrn: Option[String] = None,
    SpcId: Option[String] = None,
    LbvId: Option[String] = None
  )

  object RcDemographics {
    implicit val encoder: Encoder[RcDemographics] = deriveEncoder(derivation.renaming.snakeCase, None)
    implicit val decoder: Decoder[RcDemographics] = deriveDecoder(derivation.renaming.snakeCase, true, None)
  }

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

}

class TestREDCapClientSpec extends Specification {

  val ec = scala.concurrent.ExecutionContext.global
  implicit val timer = IO.timer(ec)
  implicit val cs = IO.contextShift(ec)  // Need cats.effect.ContextShift[cats.effect.IO] because not inside of IOApp

  import io.circe.generic.auto._
  import TestFixtures._

  "REDCap Client Resource" should {
    "Create usable client" in {

      createREDCapClientResource[IO]("local").use {
        case apiAggregator =>

          apiAggregator.createProject(proj, projectId)
              .flatMap { token =>
                apiAggregator.apiService.exportData[Metadata](Chain("token" -> token.getOrElse(""), "content" -> "metadata"))
                  .flatMap {
                    in =>
                      in match {
                        case Right(m) => println(m)
                        case Left(e) => println(e.show)
                      }

                      Stream.emit(())
                  }
              }

              .compile.drain.unsafeRunSync()

          IO.unit
      }.unsafeRunSync()

      1 mustEqual 1
    }

    "Create new project with ODM" in {

      createREDCapClientResource[IO]("local").use { case apiAggregator =>

        import config._
        val prog = apiAggregator.createProject(proj, projectId)
        prog.compile.toList.unsafeRunSync()

        IO.unit

      }.unsafeRunSync()

      1 mustEqual 1
    }

    "Find a record with a known key" in {
      createREDCapClientResource[IO]("local").use {
        case apiAggregator =>

          apiAggregator.createProject(proj, projectId)
            .flatMap { token =>
              apiAggregator.apiService.exportData[List[RcDemographics]](record("demographics") ++ Chain("token" -> token.getOrElse("")))
                .flatMap {
                  in =>
                    in match {
                      case Right(m) => println(m)
                      case Left(e) => println(e.show)
                    }
                    Stream.emit(())
                }
            }
            .compile.drain.unsafeRunSync()

          IO.unit
      }.unsafeRunSync()

      1 mustEqual 1
    }

    "Find repeat instruments with a known key" in {
      def handleCreateProject[F[_]](apiAggregator: ApiAggregator[F]): Option[String] => Stream[F, ApiResp] =
        token =>
          apiAggregator.apiService.exportData[List[RcSpecimen]](record("research_specimens") ++ Chain("token" -> token.getOrElse("")))
            .flatMap(handleExportResponse(token, apiAggregator))

      def handleExportResponse[F[_]](token: Option[String], apiAggregator: ApiAggregator[F]): Either[Chain[String], List[RcSpecimen]] => Stream[F, ApiResp] =
        in => in match {
          case Right(m) =>

            val inst = com.eztier.common.Util.stringToInstant("2020-04-28 15:30:34", Some("yyyy-MM-dd HH:mm:ss"))
            val sec0 = inst.getEpochSecond

            val sec1 = m.map(a => a.SpecModifyDate.getOrElse(Instant.now).getEpochSecond).sorted
              .zipWithIndex
              .reverse.headOption.getOrElse((0L, 0))

            sec0 mustEqual 1588102234
            // sec0 shouldEqual sec1._2

            // Try import.
            val spec0 = RcSpecimen(
              RecordId = "ABCDEFG".some,
              RedcapRepeatInstance = (sec1._2 + 2).some,
              SpecDate = LocalDate.parse("2020-02-03").some,
              SpecModifyDate = Instant.now.some
            )

            apiAggregator.apiService.importData[List[RcSpecimen]] (List(spec0), Chain("content" -> "record") ++ Chain("token" -> token.getOrElse("")))

          case Left(e) =>
            println(e.show)
            Stream.emit(ApiError(Json.Null, e.show)).covary[F]
        }

      createREDCapClientResource[IO]("local").use {
        case apiAggregator =>

          apiAggregator.createProject(proj, projectId)
            .flatMap(handleCreateProject(apiAggregator))
            // flatMap(handleExportResponse(apiAggregator))
            .through(_.map(println(_)))
            .compile.drain.unsafeRunSync()

          IO.unit
      }.unsafeRunSync()

      1 mustEqual 1
    }

  }

}
