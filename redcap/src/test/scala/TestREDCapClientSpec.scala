package com.eztier
package redcap.client
package test

import cats.implicits._
import cats.data.Chain
import cats.effect.IO
import fs2.{io, text, Stream}
import java.nio.file.Paths

import org.specs2.mutable._

import domain._

class TestREDCapClientSpec extends Specification {

  val ec = scala.concurrent.ExecutionContext.global
  implicit val timer = IO.timer(ec)
  implicit val cs = IO.contextShift(ec)  // Need cats.effect.ContextShift[cats.effect.IO] because not inside of IOApp

  import io.circe.generic.auto._

  "REDCap Client Resource" should {
    "Create usable client" in {

      createREDCapClientResource[IO].use {
        case apiService =>

          apiService.exportData[Metadata](Chain(("content" -> "metadata")))
              .flatMap {
                in =>
                  in match {
                    case Right(m) => println(m)
                    case Left(e) => println(e.show)
                  }

                  Stream.emit(())
              }
              .compile.drain.unsafeRunSync()

          IO.unit
      }.unsafeRunSync()

      1 mustEqual 1
    }

    "Create new project with ODM" in {

      val odmFilePath = System.getProperty("user.dir") + "/test-odm-template.xml"

      val proj = Project(
        ProjectTitle = "Template Test 01 API",
        Purpose = Some(4),
        ProjectNotes = "20-XXXXXX"
      )
      
      createREDCapClientResource[IO].use {
        case apiService =>

          apiService.readAllFromFile(odmFilePath)
            .flatMap { x =>
              apiService.importData[Project](Chain(
                ("content" -> "project"), ("odm" -> "")
              )).flatMap {
                in =>
                  in match {
                    case Right(m) => println(m)
                    case Left(e) => println(e.show)
                  }

                  Stream.emit(())
              }
            }.compile.drain.unsafeRunSync()

        }.unsafeRunSync()

    }
  }

}
