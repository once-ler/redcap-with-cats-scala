package com.eztier
package redcap.entity
package test

import java.time.Instant

import cats.data._
import cats.implicits._
import cats.effect.IO
import fs2.Stream
import org.specs2.mutable.Specification
import com.eztier.common.{CSVConfig, CSVConverter, CaseClassFromMap}
import com.eztier.redcap.client.createREDCapClientResource
import com.eztier.redcap.client.domain.{ApiError, ProjectToken}
import com.eztier.redcap.entity.limsmock.domain.types.{LimsSpecimen, RcSpecimen}
import com.eztier.redcap.entity.limsmock.createLvToRcAggregatorResource
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

      createLvToRcAggregatorResource[IO].use {
        case lvToRcAggregator =>

          val s = for {
            x <- lvToRcAggregator.limsSpecimenService.list()
            y = x.filter(_.REDCAPID.isDefined).groupBy(_.REDCAPID.get).toList
            z = Stream.emits(y)
              .flatMap { case (key, vals) =>

                // Get token for project.
                val ns = for {
                  maybeToken <- lvToRcAggregator
                    .apiAggregator
                    .tokenService.findById(key.some)
                    .fold(_ => ProjectToken().some, a => a)

                  token = maybeToken.get.token

                  s0 <- Stream.emits(vals).covary[IO]
                    .chunkN(100)
                    .flatMap { s =>

                      val u = s.map { t =>
                        val recordId = if (t.USE_STUDYLINKID.getOrElse(0) == 1) t.STUDYLINKID else t.U_MRN

                        sampleValueToRcSpecimen(t.SAMPLEVALUE)
                          .getOrElse(RcSpecimen())
                          .copy(RecordId = recordId, SpecDate = t.SAMPLE_COLLECTION_DATE, SpecModifyDate = t.MODIFYDATE)
                      }.filter(_.SpecModifyDate.isDefined).toList

                      Stream.emits(u).covary[IO]
                    }

                  s1 <- {
                    val recordId = s0.RecordId
                    val body = record("research_specimens".some, recordId) ++ Chain("token" -> token.getOrElse(""))

                    lvToRcAggregator
                      .apiAggregator
                      .apiService
                      .exportData[List[RcSpecimen]](body)
                      .flatMap { m =>
                        m match {
                          case Right(l) =>

                            val sec1 = l.map(a => a.SpecModifyDate.getOrElse(Instant.now).getEpochSecond).sorted
                              .zipWithIndex
                              .reverse.headOption.getOrElse((0L, -1))

                            val n = l.find(a => a.RecordId.eqv(recordId)) match {
                              case Some(b) =>
                                // Update
                                s0.copy(
                                  RedcapRepeatInstance = b.RedcapRepeatInstance
                                )
                              case None =>
                                // Insert
                                s0.copy(
                                  RedcapRepeatInstance = (sec1._2 + 2).some
                                )
                            }

                            lvToRcAggregator
                              .apiAggregator
                              .apiService
                              .importData[List[RcSpecimen]] (List(n), Chain("content" -> "record") ++ Chain("token" -> token.getOrElse("")))
                          case Left(e) =>
                            // Report error.
                            // println(e.show)
                            Stream.emit(ApiError(Json.Null, e.show)).covary[IO]
                        }
                      }
                  }
                } yield s1

                  

                Stream.emit(()).covary[IO]
              }
          } yield ()

          IO.delay(s.unsafeRunSync())

          // IO.unit
      }.unsafeRunSync()

      1 mustEqual 1
    }

  }
}

