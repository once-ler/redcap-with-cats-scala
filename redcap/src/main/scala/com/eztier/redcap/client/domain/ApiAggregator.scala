package com.eztier.redcap.client
package domain

import cats.implicits._
import cats.Functor
import cats.data.Chain
import fs2.{Pipe, Stream}
import io.circe.Encoder

class ApiAggregator[F[_]: Functor](
  val apiService: ApiService[F],
  val tokenService: ProjectTokenService[F]
) {

  private def toImportProjectPipeS(data: Project): Pipe[F, String, ApiResp] = // Stream[F, String] => Stream[F, ApiResp] =
    s =>
      for {
        y <- s.flatMap {
          x =>
            apiService.importData[List[Project]] (List(data), Chain("content" -> "project", "odm" -> x))
        }
      } yield y

  private def toPersistProjectToken(projectId: Option[String]): Pipe[F, Option[String], Option[String]] =
    _.evalMap { newTk =>
      tokenService.insertMany(List(ProjectToken(
        project_id = projectId,
        token = newTk
      ))).map(_ => newTk)
    }

  private def createProjectImpl(data: Project, projectId: Option[String])(implicit ev: Encoder[Project]): Stream[F, Option[String]] = {
    val a = Stream.eval(apiService.showConf)

    for {
      y <- a.flatMap { conf =>
        Stream.eval(apiService.readAllFromFile(conf.odm.getOrElse("")))
          .through(toImportProjectPipeS(data))
          .flatMap { in =>
            // TODO: logging
            Stream.emit(in).covary[F]
          }
          .flatMap { in =>
            val newTk = in match {
              case ApiOk(body) => println(body)
                // Response body is a string, without braces.
                body.toString.replaceAllLiterally("\"", "").some
              case ApiError(body, error) => println(body, error)
                None
            }
            Stream.emit(newTk)
          }
          .through(toPersistProjectToken(projectId))
      }
    } yield y

  }

  def createProject(data: Project, projectId: Option[String])(implicit ev: Encoder[Project]): Stream[F, Option[String]] = {

    val a = Stream.eval(tokenService
      .findById(projectId)
      .fold(_ => None, a => a))

    for {
      y <- a.flatMap { x =>
        x match {
          case Some(_) => Stream.emit(x.get.token).covary[F]
          case None => createProjectImpl(data, projectId)
        }
      }
    } yield y

  }

}

object ApiAggregator {
  def apply[F[_]: Functor](
    apiService: ApiService[F],
    tokenService: ProjectTokenService[F]
  ): ApiAggregator[F] =
    new ApiAggregator[F](apiService, tokenService)
}
