package com.eztier.redcap
package entity.limsmock.infrastructure
package api

import cats.implicits._
import cats.Functor
import cats.data.Chain
import cats.effect.{ConcurrentEffect, ContextShift}
import com.eztier.redcap.entity.limsmock.domain.types.{LimsSpecimen, RcDemographics}
import fs2.{Pipe, Stream}
import io.circe.{Decoder}
import io.circe.syntax._
import client.domain.{ApiAggregator, ApiOk, ApiResp, Project}

class REDCapInterpreter[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
(
  apiAggregator: ApiAggregator[F]
) {

  import io.circe.generic.auto._

  private def tryExport[A](in: A, options: Chain[(String, String)]): Pipe[F, Option[String], ApiResp] =
    _.flatMap { a =>
      val nextOptions = options ++ Chain("token" -> a.getOrElse(""))
      
      apiAggregator.apiService.exportData[A](nextOptions)
        .flatMap { r =>
          r match {
            case Right(b) =>
              Stream.emit(ApiOk(b.asJson)).covary[F]
            case Left(e) =>
              println(e) // Log
              apiAggregator.apiService.importData[A](in, nextOptions)
          }
        }
    }

  def fetch[A](in: LimsSpecimen)(implicit ev: Decoder[A]) = {
    val partid = if (in.USE_STUDYLINKID.getOrElse(false)) in.STUDYLINKID.getOrElse("") else in.U_MRN.getOrElse("")

    apiAggregator
      .createProject(
        Project(
          ProjectTitle = in.SSTUDYID,
          Purpose = Some(4),
          ProjectNotes = in.SSTUDYID,
          ProjectIrbNumber = in.SSTUDYID
        ), in.SSTUDYID
      ).through(
        tryExport[RcDemographics](
          RcDemographics(
            RecordId = partid.some,
            Mrn = in.U_MRN,
            SpcId = in.STUDYLINKID
          ),
          Chain(
            "content" -> "record",
            "fields" -> "record_id,mrn,spc_id,lbv_id",
            "forms" -> "demographics",
            "filterLogic" -> s"[record_id] = '$partid'"
          )
        )
      )




  }
}
