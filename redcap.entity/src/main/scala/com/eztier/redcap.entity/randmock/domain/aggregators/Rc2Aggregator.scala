package com.eztier.redcap.entity.randmock
package domain
package aggregators

import java.time.{Instant, LocalDate}

import cats.implicits._
import cats.Functor
import cats.data.Chain
import cats.effect.{ConcurrentEffect, ContextShift}
import com.eztier.redcap.entity.limsmock.domain.types.{LimsSpecimen, RcDemographics}
import fs2.{Pipe, Stream}
import io.circe.{Decoder, Json}
import io.circe.syntax._
import scala.util.Try

import domain.types._
import com.eztier.redcap._
import client.domain.{ApiAggregator, ApiError, ApiOk, ApiResp}

class Rc2Aggregator[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
(
  localApiAggregator: ApiAggregator[F],
  remoteApiAggregator: ApiAggregator[F],
  localForm: Option[String],
  remoteForm: Option[String]
) {

  private val record: (String, String) => Chain[(Option[String], Option[String])] =
    (forms, patid) => Chain(
      "content" -> "record",
      "forms" -> forms.getOrElse("")
    ) ++ patid match {
      case Some(a) => Chain("records" -> patid.some)
      case None => Chain.empty[(Option[String], Option[String])]
    }

  private def handleFetch[A, B]: Either[Chain[String], List[A]] => Stream[F, ApiResp] =
    in => in match {
      case Right(m) =>
        val n = m.map {

        }
        remoteApiAggregator.apiService.importData[List[B]] (n, Chain("content" -> "record"))

      case Left(e) =>
        println(e.show)
        Stream.emit(ApiError(Json.Null, e.show)).covary[F]
    }

  def fetch[A](implicit ev: Decoder[A]) = {
    localApiAggregator.exportData[List[A]](record(localForm))
  }
}

object Rc2Aggregator {
  def apply[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
  (
    localApiAggregator: ApiAggregator[F],
    remoteApiAggregator: ApiAggregator[F]
  ): Rc2Aggregator[F] =
  new Rc2Aggregator(
    localApiAggregator,
    remoteApiAggregator
  )
}
