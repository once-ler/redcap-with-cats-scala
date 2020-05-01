package com.eztier.redcap
package entity.limsmock
package infrastructure.api

import java.time.{Instant, LocalDate}

import cats.implicits._
import cats.Functor
import cats.data.Chain
import cats.effect.{ConcurrentEffect, ContextShift}
import com.eztier.redcap.entity.limsmock.domain.types.{LimsSpecimen, RcDemographics}
import fs2.{Pipe, Stream}
import io.circe.{Decoder, Json}
import io.circe.syntax._
import client.domain.{ApiAggregator, ApiError, ApiOk, ApiResp, Project}
import domain.types._

import scala.util.Try

class REDCapInterpreter[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
(
  apiAggregator: ApiAggregator[F]
) {

  private val record: (String, String) => Chain[(String, String)] =
    (forms, patid) => Chain(
      "content" -> "record",
      "forms" -> forms,
      "records" -> patid
    )

  private def handleCreateProject[F[_]](apiAggregator: ApiAggregator[F], in: LimsSpecimen, recordId: String): Option[String] => Stream[F, ApiResp] =
    token =>
      apiAggregator.apiService.exportData[List[RcSpecimen]](record("research_specimens", recordId) ++ Chain("token" -> token.getOrElse("")))
        .flatMap(handleExportResponse(token, apiAggregator, in, recordId))

  private def handleExportResponse[F[_]](token: Option[String], apiAggregator: ApiAggregator[F], limsRec: LimsSpecimen, recordId: String): Either[Chain[String], List[RcSpecimen]] => Stream[F, ApiResp] =
    in => in match {
      case Right(m) =>

        // TODO: Check whether record instrument already exist.
        // m.find(a => a.RecordId)

        val sec1 = m.map(a => a.SpecModifyDate.getOrElse(Instant.now).getEpochSecond).sorted
          .zipWithIndex
          .reverse.headOption.getOrElse((0L, -1))

        // Try import.
        val spec0 = RcSpecimen(
          RecordId = recordId.some,
          RedcapRepeatInstance = (sec1._2 + 2).some,
          SpecDate = limsRec.SAMPLE_COLLECTION_DATE,
          SpecModifyDate = limsRec.MODIFYDATE
        )

        apiAggregator.apiService.importData[List[RcSpecimen]] (List(spec0), Chain("content" -> "record") ++ Chain("token" -> token.getOrElse("")))

      case Left(e) =>
        println(e.show)
        Stream.emit(ApiError(Json.Null, e.show)).covary[F]
    }

  def fetch[A](in: LimsSpecimen)(implicit ev: Decoder[A]) = {
    val recordId = if (in.USE_STUDYLINKID.getOrElse(0) == 1) in.STUDYLINKID.getOrElse("") else in.U_MRN.getOrElse("")

    val proj = Project(
      ProjectTitle = in.SSTUDYID,
      Purpose = Some(4),
      ProjectNotes = in.SSTUDYID,
      ProjectIrbNumber = in.SSTUDYID
    )

    apiAggregator.createProject(proj, in.SSTUDYID)
      .flatMap(handleCreateProject(apiAggregator, in, recordId))
  }
}
