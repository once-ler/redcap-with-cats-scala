package com.eztier.redcap.entity.randmock
package domain
package aggregators

import java.time.{Instant, LocalDate}

import cats.implicits._
import cats.Functor
import cats.data.Chain
import cats.effect.{ConcurrentEffect, ContextShift, Sync}
import fs2.{Pipe, Stream}
import io.circe.{Decoder, Json}
import io.circe.syntax._
import io.circe.generic.auto._

import scala.util.Try
import domain.types.{RcLocalRandomization, RcRemoteRandomization}
import com.eztier.redcap._
import client.domain.{ApiAggregator, ApiError, ApiOk, ApiResp}
import com.eztier.common.MonadLog

class Rc2Aggregator[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
(
  localApiAggregator: ApiAggregator[F],
  remoteApiAggregator: ApiAggregator[F],
  localForm: Option[String],
  remoteForm: Option[String]
)(implicit logs: MonadLog[F, Chain[String]]) {

  private def record(forms: Option[String], patid: Option[String] = None, filter: Option[String] = None): Chain[(String, String)] =
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

  private def persistToRemote(l: List[RcLocalRandomization]): Either[Chain[String], List[RcRemoteRandomization]] => Stream[F, ApiResp] =
    d => d match {
      case Right(o) =>

        val n = o.map { p =>
          val a = l.find(_.SubjectId.eqv(p.RecordId)).get

          RcRemoteRandomization(
            RecordId = a.SubjectId,
            Rmyn = a.Eligible,
            Rmrdate = a.Randodat,
            Ragroup = a.RandoAss
          )
        }
        remoteApiAggregator.apiService.importData[List[RcRemoteRandomization]] (n, Chain("content" -> "record"))

      case Left(e) =>
        Stream.emit(ApiError(Json.Null, e.show)).covary[F]
    }

  private def handleFetch[A <: RcLocalRandomization, B <: RcRemoteRandomization]: Either[Chain[String], List[A]] => Stream[F, ApiResp] =
    in => in match {
      case Right(m) =>
        for {
          x <- Stream.emits(m).covary[F]
            .chunkN(10)
              .flatMap { c =>
                val l = c.toList
                println(l)
                remoteApiAggregator.apiService.exportData[List[RcRemoteRandomization]](record(remoteForm, l.map(_.SubjectId.getOrElse("")).mkString(",").some))
                  .flatMap(persistToRemote(l))
              }
        } yield x

      case Left(e) =>
        println(e.show)
        Stream.emit(ApiError(Json.Null, e.show)).covary[F]
    }

  def fetch[A <: RcLocalRandomization](implicit ev: Decoder[A]) = {
    val filter = "[eligible] = '1' and [randodat] <> ''"
    localApiAggregator.apiService.exportData[List[A]](record(localForm, None, filter.some))
      .flatMap(handleFetch)
  }

  def showLog: F[String] =
    for {
      l0 <- localApiAggregator.apiService.showLog
      l1 <- remoteApiAggregator.apiService.showLog
      l <- logs.get
      x = l0 + l1 + l.show
    } yield x
}

object Rc2Aggregator {
  def apply[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
  (
    localApiAggregator: ApiAggregator[F],
    remoteApiAggregator: ApiAggregator[F],
    localForm: Option[String],
    remoteForm: Option[String]
  )(implicit logs: MonadLog[F, Chain[String]]): Rc2Aggregator[F] =
  new Rc2Aggregator(
    localApiAggregator,
    remoteApiAggregator,
    localForm,
    remoteForm
  )
}
