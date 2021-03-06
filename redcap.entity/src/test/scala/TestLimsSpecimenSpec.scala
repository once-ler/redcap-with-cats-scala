package com.eztier
package redcap.entity
package test

import java.time.Instant

import cats.data._
import cats.implicits._
import cats.effect.{IO, Sync}
import fs2.{Pipe, Stream}
import org.specs2.mutable.Specification
import com.eztier.common.{CSVConfig, CSVConverter, CaseClassFromMap}
import com.eztier.redcap.client.createREDCapClientResource
import com.eztier.redcap.client.domain.{ApiError, ApiResp, ProjectToken}
import com.eztier.redcap.entity.limsmock.domain.types.{LimsSpecimen, RcSpecimen}
import com.eztier.redcap.entity.limsmock.createLvToRcAggregatorResource
import com.eztier.redcap.entity.limsmock.domain.aggregators.LvToRcAggregator
import io.circe.Json

class TestLimsSpecimenSpec extends Specification {

  val ec = scala.concurrent.ExecutionContext.global
  implicit val timer = IO.timer(ec)
  implicit val cs = IO.contextShift(ec) // Need cats.effect.ContextShift[cats.effect.IO] because not inside of IOApp

  "REDCap Client Resource" should {

    val csvFilePath = s"${System.getProperty("user.dir")}/../tmp/db/test.tsv"

    "Create usable client" in {

      createLvToRcAggregatorResource[IO].use {
        case lvToRcAggregator =>
          // Read entire file.  Not a good idea.
          // val a = lvToRcAggregator.apiAggregator.apiService.readAllFromFile(csvFilePath).unsafeRunSync()
          val s = lvToRcAggregator.apiAggregator.apiService.readByLinesFromFile(csvFilePath)

          s.chunkN(10)
            .flatMap { c =>
              val a = c.toList.mkString("\n")

              implicit val csvconf = CSVConfig(delimiter= '\t')

              val l = CSVConverter[List[LimsSpecimen]]
                .from(Some(a).fold("")(a => a))
                .fold(_ => List[LimsSpecimen](), s => s)

              println(l.map(_.SSTUDYID.get).mkString(","))
              println(l.length)

             //  val r = lvToRcAggregator.limsSpecimenService.insertMany(l).unsafeRunSync()
              Stream.emit(()).covary[IO]
            }.compile.drain.unsafeRunSync()

          IO.unit
      }.unsafeRunSync()

      1 mustEqual 1
    }

    "Convert LimsSpecimen to RcSpecimen" in {

      def record(forms: Option[String], patid: Option[String] = None, filter: Option[String] = None): Chain[(String, String)] =
        Chain(
          "content" -> "record",
          "forms" -> forms.getOrElse("")
        ) ++ (patid match {
          case Some(a) => Chain("records" -> a)
          case None => Chain.empty[(String, String)]
        }) ++ (filter match {
          case Some(a) => Chain("filterLogic" -> a)
          case None => Chain.empty[(String, String)]
        })

      val lvToRcKeyMap = Map[String, String](
        "LV ParticipantID" -> "spec_lv_participant_id",
        "EVENT" -> "spec_event",
        "SPR" -> "spec_spr",
        "SAMPLE_PARENTID" -> "spec_sample_parent_id",
        "TISSUE_STATUS" -> "spec_tissue_status",
        "QUANTITY" -> "spec_quantity",
        "UNIT" -> "spec_unit",
        "CONTAINER_TYPE" -> "spec_container_type",
        "STORAGE_STATUS" -> "spec_storage_status",
        "LOCATION" -> "spec_location"
      )

      val sampleValueToRcSpecimen: Option[String] => Option[RcSpecimen] =
        sampleValue => {
          val kv =
            sampleValue
              .getOrElse("")
              .split(';')
              .map(_.split(':'))
              .filter(_.length == 2)
              .foldRight(Map[String, Any]())((a, m) =>
                m ++ (lvToRcKeyMap.find(_._1 == a(0)) match {
                  case Some((k, k1)) => Map(k1 -> a(1))
                  case _ => Map.empty[String, Any]
                })
              )

          CaseClassFromMap.mapToCaseClass[RcSpecimen](kv) match {
            case Right(a) => Some(a)
            case _ => None
          }
        }

      def sampleValueToRcSpecimenPipeS[F[_]: Sync](vals: List[LimsSpecimen]): Stream[F, List[RcSpecimen]] =
          Stream.emits(vals)
            .covary[F]
            .chunkN(100)
            .evalMap { s =>
              Sync[F].delay(
                s.map { t =>
                  val recordId = if (t.USE_STUDYLINKID.getOrElse(0) == 1) t.STUDYLINKID else t.U_MRN

                  sampleValueToRcSpecimen(t.SAMPLEVALUE)
                    .getOrElse(RcSpecimen())
                    .copy(RecordId = recordId, SpecDate = t.SAMPLE_COLLECTION_DATE, SpecModifyDate = t.MODIFYDATE, SpecSampleKey = t.SAMPLEKEY)
                }.filter(_.SpecModifyDate.isDefined).toList
              )
            }

      def tryPersistListRcSpecimenImplPipeS[F[_]: Sync](vals: List[(String, List[RcSpecimen])], lvToRcAggregator: LvToRcAggregator[F], token: Option[String]): Stream[F, ApiResp] =
      {

        Stream.emits(vals)
          .covary[F]
          .flatMap[F, ApiResp] { case (recordId, samples) =>
            val body = record("research_specimens".some, recordId.some) ++ Chain("token" -> token.getOrElse(""))

            lvToRcAggregator
              .apiAggregator
              .apiService
              .exportData[List[RcSpecimen]](body)
              .flatMap[F, ApiResp] { m =>
              m match {
                case Right(l) =>
                  val samplesSorted = samples.sortBy(_.SpecSampleKey.getOrElse(""))

                  val sec1 = l.map(a => a.SpecSampleKey.getOrElse("")).sorted
                    .zipWithIndex

                  val sec2 = sec1.reverse.headOption.getOrElse(("", -1))

                  val ln = samplesSorted.foldLeft((sec2._2, List.empty[RcSpecimen])) { case (agg, s0) =>
                    val (inc, lst) = agg
                    sec1.find(a => a._1 == s0.SpecSampleKey.getOrElse("")) match {
                      case Some((_, idx)) =>
                        // Update
                        (inc, lst ++ List(s0.copy(
                          RedcapRepeatInstance = (idx + 1).some
                        )))
                      case None =>
                        // Insert
                        (inc + 1, lst ++List(s0.copy(
                          RedcapRepeatInstance = (inc + 2).some
                        )))
                    }
                  }

                  lvToRcAggregator
                    .apiAggregator
                    .apiService
                    .importData[List[RcSpecimen]] (ln._2, Chain("content" -> "record") ++ Chain("token" -> token.getOrElse("")))
                case Left(e) =>
                  // Report error.
                  // println(e.show)
                  Stream.emit(ApiError(Json.Null, e.show)).covary[F]
              }
            }

          }
      }

      def tryPersistRcSpecimenPipeS[F[_]: Sync](vals: List[LimsSpecimen], lvToRcAggregator: LvToRcAggregator[F]): Option[ProjectToken] => Stream[F, ApiResp] =
        maybeToken => {
          val token = maybeToken.getOrElse(ProjectToken()).token

          sampleValueToRcSpecimenPipeS(vals)
            .flatMap[F, ApiResp] { s0 =>

            // Group by RecordId, fetch instruments for form 1 time.
            val s1 = s0.groupBy(_.RecordId.getOrElse(""))
              .mapValues(_.sortBy(_.SpecSampleKey.getOrElse("")))
              .toList

            tryPersistListRcSpecimenImplPipeS(s1, lvToRcAggregator, token)
          }
        }

      def fetchNext = ???
      /*
      def fetchNext[F[_]: Sync](lvToRcAggregator: LvToRcAggregator[F]): Stream[F, ApiResp] =
        Stream.eval(lvToRcAggregator.localLimsSpecimenService.list())
          .flatMap[F, ApiResp] { x =>
            val y = x.filter(_.REDCAPID.isDefined).groupBy(_.REDCAPID.get)
              .mapValues(_.sortBy(_.SAMPLEKEY.getOrElse("")))
              .toList

            Stream.emits(y)
              .covary[F]
              .flatMap[F, ApiResp] { case (key, vals) =>
                Stream.eval(lvToRcAggregator.apiAggregator.getProjectToken(key.some))
                  .flatMap[F, ApiResp](tryPersistRcSpecimenPipeS(vals, lvToRcAggregator))
              }
          }
      */

      def handlePersistResponse[F[_]: Sync]: Stream[F, ApiResp] => Stream[F, Unit] =
        s =>
          s.map { a =>
            println(a)
            Stream.empty.covary[F]
          }

      createLvToRcAggregatorResource[IO].use {
        case lvToRcAggregator =>
          // IO.delay(fetchNext[IO](lvToRcAggregator).through(handlePersistResponse).compile.drain.unsafeRunSync())
          IO.unit
      }.unsafeRunSync()

      1 mustEqual 1
    }

  }

  "LimsSpecimen to RcSpecimen Aggregator" should {
    "Decode array of RcSpecimen" in {
      import io.circe.parser._

      val incomingData = """[{
        "spec_lv_participant_id": "P-XXXXX740",
        "spec_event": "Time point 1",
        "spec_date": "2014-10-20",
        "spec_sample_type": "SOMETHING",
        "spec_sample_key": "Z-XXXXYYY-00001",
        "spec_tissue_status": "",
        "spec_anatomic_site": "XYZ",
        "spec_surgical_path_nbr": "",
        "spec_quantity": "2",
        "spec_unit": "ml",
        "spec_storage_status": "Disposed",
        "spec_location": "",
        "spec_modify_date": "2018-03-23 20:42:09",
        "research_specimens_complete": "0"
      }]"""

      val x = parse(incomingData).getOrElse(Json.fromString(incomingData))

      val maybeA = x.as[List[RcSpecimen]]

      maybeA match {
        case Right(a) =>
          println(a)
        case Left(e) =>
          println(e)
      }

      1 mustEqual 1
    }

    "Run Unprocessed" in {
      createLvToRcAggregatorResource[IO].use {
        case lvToRcAggregator =>

          val io1 = lvToRcAggregator
            .runUnprocessed
            .compile
            .drain

          val io2 = lvToRcAggregator
            .fetchNext
            .compile
            .drain

          val io = IO.suspend(io1 *> io2)

          io.unsafeRunSync()

          IO.unit
          /*
          IO.delay(lvToRcAggregator
            .runUnprocessed
            .compile
            .drain
            .unsafeRunSync())
          */
      }.unsafeRunSync()

      1 mustEqual 1
    }
  }
}

