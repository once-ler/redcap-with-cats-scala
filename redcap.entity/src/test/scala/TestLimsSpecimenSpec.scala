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
import com.eztier.redcap.entity.limsmock.domain.types.LimsSpecimen

class TestLimsSpecimenSpec extends Specification {

  val ec = scala.concurrent.ExecutionContext.global
  implicit val timer = IO.timer(ec)
  implicit val cs = IO.contextShift(ec) // Need cats.effect.ContextShift[cats.effect.IO] because not inside of IOApp

  "REDCap Client Resource" should {
    "Create usable client" in {

      createREDCapClientResource[IO]("local").use {
        case apiAggregator =>

          val csvFile = s"${System.getProperty("user.dir")}/../tmp/db/test.tsv"
          apiAggregator.apiService.readAllFromFile(csvFile)
              .flatMap{ a =>
                println(a)

                import com.eztier.common.CSVConverter._

                implicit val csvconf = CSVConfig(delimiter= '\t')

                val l = csvToCC(CSVConverter[List[LimsSpecimen]], Some(a), LimsSpecimen())

                Stream.emit(()).covary[IO]
              }.compile.drain.unsafeRunSync()

          IO.unit
      }.unsafeRunSync()

      1 mustEqual 1
    }
  }
}

