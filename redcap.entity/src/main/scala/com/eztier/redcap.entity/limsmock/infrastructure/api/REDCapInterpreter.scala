package com.eztier.redcap
package entity.limsmock.infrastructure
package api

import cats.implicits._
import cats.Functor
import cats.data.Chain
import cats.effect.{ConcurrentEffect, ContextShift}
import com.eztier.redcap.entity.limsmock.domain.types.{LimsSpecimen, RcDemographics}
import fs2.{Pipe, Stream}
import io.circe.Decoder
import client.domain.{ApiAggregator, Project}

class REDCapInterpreter[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
(
  apiAggregator: ApiAggregator[F]
) {

  import io.circe.generic.auto._

  private def tryExport[A](options: Chain[(String, String)], in: A): Pipe[F, Option[String], Option[A]] =
    _.flatMap { a =>
      apiAggregator.apiService.exportData[A](options)
        .flatMap { r =>
          val n = r match {
            case Right(b) => b.some
            case Left(e) =>
              println(e) // Log
              None
          }
          Stream.emit(n).covary[F]
        }
    }

  def fetch[A](in: LimsSpecimen)(implicit ev: Decoder[A]) = {
    val partid = if (in.USE_STUDYLINKID.getOrElse(false)) in.STUDYLINKID.getOrElse("") else in.U_MRN.getOrElse("")

    apiAggregator.createProject(
      Project(
        ProjectTitle = in.SSTUDYID,
        Purpose = Some(4),
        ProjectNotes = in.SSTUDYID,
        ProjectIrbNumber = in.SSTUDYID
      ), in.SSTUDYID
    ).through(tryExport[RcDemographics](
      Chain(
        "content" -> "record",
        "fields" -> "record_id,mrn,spc_id,lbv_id",
        "forms" -> "demographics",
        "filterLogic" -> s"[record_id] = '$partid'")
      , RcDemographics(
        RecordId = partid.some,
        Mrn = in.U_MRN,
        SpcId = in.STUDYLINKID
      )
    ))




  }
}
