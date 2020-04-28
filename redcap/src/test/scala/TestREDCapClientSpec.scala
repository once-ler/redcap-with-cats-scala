package com.eztier
package redcap.client
package test

import cats.implicits._
import cats.data.Chain
import cats.effect.{IO, Sync}
import fs2.Stream
import org.specs2.mutable._
import domain._
import io.circe.{Encoder, Json}
import io.circe.syntax._
import org.http4s.{Header, Headers}
// import io.circe.generic.semiauto._

object TestFixtures {
  val odmFilePath = System.getProperty("user.dir") + "/internal/test-odm-template.xml"

  val projectId = "20-XXXXXX".some
  val proj = Project(
    ProjectTitle = projectId,
    Purpose = Some(4),
    ProjectNotes = projectId,
    ProjectIrbNumber = projectId
  )

  val patid = "ABCDEFG"
  val record0 = Chain(
    "content" -> "record",
    "fields" -> "record_id,mrn,spc_id",
    "forms" -> "demographics",
    "filterLogic" -> s"[record_id] = '$patid'")

}

class TestREDCapClientSpec extends Specification {

  val ec = scala.concurrent.ExecutionContext.global
  implicit val timer = IO.timer(ec)
  implicit val cs = IO.contextShift(ec)  // Need cats.effect.ContextShift[cats.effect.IO] because not inside of IOApp

  import io.circe.generic.auto._
  import TestFixtures._

  "REDCap Client Resource" should {
    "Create usable client" in {

      createREDCapClientResource[IO].use {
        case apiAggregator =>

          val apiService = apiAggregator.apiService

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

      createREDCapClientResource[IO].use { case apiAggregator =>

        import config._
        val prog = apiAggregator.createProject(proj, projectId)
        prog.compile.toList.unsafeRunSync()

        IO.unit

      }.unsafeRunSync()

      1 mustEqual 1
    }

    "Find a record with a known key" in {

      createREDCapClientResource[IO].use {
        case apiAggregator =>

          apiAggregator.apiService.exportData[Json](record0)
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

  }

}
