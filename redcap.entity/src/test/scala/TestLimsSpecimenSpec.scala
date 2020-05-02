package com.eztier
package redcap.entity
package test

import shapeless._
import cats.effect.IO
import fs2.Stream
import org.specs2.mutable.Specification
import com.eztier.common.Util.csvToCC
import com.eztier.common.{CSVConfig, CSVConverter, CaseClassFromMap}
import com.eztier.redcap.client.createREDCapClientResource
import com.eztier.redcap.entity.limsmock.domain.types.{LimsSpecimen, RcSpecimen}
import com.eztier.redcap.entity.limsmock.createLvToRcAggregatorResource

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

      import com.eztier.common.Util

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

                Stream.emits(vals).covary[IO]
                  .chunkN(100)
                  .flatMap { s =>

                    val u = s.map { t =>
                      sampleValueToRcSpecimen(t.SAMPLEVALUE)
                        .getOrElse(RcSpecimen())
                        .copy(SpecDate = t.SAMPLE_COLLECTION_DATE, SpecModifyDate = t.MODIFYDATE)
                    }.filter(_.SpecModifyDate.isDefined)

                    // Persist to database.

                    Stream.emit(()).covary[IO]
                  }
              }.compile.drain.unsafeRunSync()
          } yield ()

          IO.delay(s.unsafeRunSync())

          // IO.unit
      }.unsafeRunSync()

      1 mustEqual 1
    }

  }
}

