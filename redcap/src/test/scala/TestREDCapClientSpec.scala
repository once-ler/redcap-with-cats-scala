package com.eztier
package redcap.client
package test

import cats.implicits._
import cats.data.Chain
import cats.effect.{IO, Sync}
import fs2.Stream
import org.specs2.mutable._
import domain._
import io.circe.Encoder
import io.circe.syntax._
import org.http4s.{Header, Headers}
// import io.circe.generic.semiauto._

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

      val odmFilePath = System.getProperty("user.dir") + "/internal/test-odm-template.xml"

      val proj = Project(
        ProjectTitle = Some("Template Test 01 API"),
        Purpose = Some(4),
        ProjectNotes = Some("20-XXXXXX")
      )
      
      createREDCapClientResource[IO].use { case apiService =>

        import config._

        val conf = for {
          c <- apiService.showConf
        } yield c

        val a = Stream.eval(conf)

        a.through(b => b.map { c =>

          println(c.asJson)

          apiService.readAllFromFile(c.odm.getOrElse(""))
            .flatMap { x =>
              apiService
                .importData[List[Project]](List(proj), Chain(("content" -> "project"), ("odm" -> x)))
                .flatMap {
                  in =>
                    in match {
                      case ApiOk(body) =>
                        println(body)
                      case ApiError(body, error) => println(body, error)
                    }

                    Stream.eval(apiService.showLog)
                }.flatMap { l =>
                  println(l)
                  Stream.emit(())
                }
              }.compile.drain.unsafeRunSync()

        }).compile.drain
/*
        apiService.readAllFromFile(odmFilePath)
          .flatMap { x =>
            apiService.importData[Project](proj, Chain(
              ("content" -> "project"), ("odm" -> x)
            )).flatMap {
              in =>
                in match {
                  case ApiOk(body) =>
                    println(body)
                  case ApiError(body, error) => println(body, error)
                }

                Stream.eval(apiService.showLog)
            }.flatMap { l =>
              println(l)
              Stream.emit(())
            }
          }.compile.drain.unsafeRunSync()
*/
          // IO.unit
        }.unsafeRunSync()

      1 mustEqual 1
    }
  }

}
