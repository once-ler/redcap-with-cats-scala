package com.eztier
package redcap.entity
package test

import shapeless._
import cats.effect.IO
import fs2.Stream
import org.specs2.mutable.Specification
import com.eztier.common.Util.csvToCC
import com.eztier.common.{CSVConfig, CSVConverter}
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

      createLvToRcAggregatorResource[IO].use {
        case lvToRcAggregator =>

          for {
            x <- lvToRcAggregator.limsSpecimenService.list()
            y = x.filter(_.REDCAPID.isDefined).groupBy(_.REDCAPID.get).toList
            z = Stream.emits(y)
              .flatMap { case (key, vals) =>

                Stream.emits(vals).covary[IO]
                  .chunkN(100)
                  .flatMap { s =>

                    s.map { t =>
                      RcSpecimen(

                      )
                    }

                    Stream.emit(()).covary[IO]
                  }
              }
          } yield ()

          IO.unit
      }

      1 mustEqual 1
    }

  }
}

