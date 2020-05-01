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
import com.eztier.redcap.entity.limsmock.createLvToRcAggregatorResource

class TestLimsSpecimenSpec extends Specification {

  val ec = scala.concurrent.ExecutionContext.global
  implicit val timer = IO.timer(ec)
  implicit val cs = IO.contextShift(ec) // Need cats.effect.ContextShift[cats.effect.IO] because not inside of IOApp

  "REDCap Client Resource" should {
    "Create usable client" in {

      createLvToRcAggregatorResource[IO].use {
        case lvToRcAggregator =>

          val csvFilePath = s"${System.getProperty("user.dir")}/../tmp/db/test.tsv"
          val csvFile = lvToRcAggregator.apiAggregator.apiService.readAllFromFile(csvFilePath).compile.toList.unsafeRunSync()
          val a = csvFile.mkString("")
          println(a)

          implicit val csvconf = CSVConfig(delimiter= '\t')

          val l = CSVConverter[List[LimsSpecimen]]
            .from(Some(a).fold("")(a => a))
            .fold(e => List[LimsSpecimen](), s => s)

          val r = lvToRcAggregator.limsSpecimenService.insertMany(l).unsafeRunSync()

          IO.unit
      }.unsafeRunSync()

      1 mustEqual 1
    }
  }
}

