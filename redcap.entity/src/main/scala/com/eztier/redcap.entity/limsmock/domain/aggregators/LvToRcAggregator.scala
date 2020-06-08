package com.eztier.redcap.entity.limsmock
package domain.aggregators

import cats._
import cats.data._
import cats.implicits._
import cats.Functor
import cats.effect.{Sync, ConcurrentEffect, ContextShift}
import fs2.{Pipe, Stream}
import io.circe.Json
import io.circe.optics.JsonPath._

import com.eztier.common.{CSVConfig, CSVConverter, CaseClassFromMap}
import com.eztier.redcap.client.domain.{ApiAggregator, ApiOk, ApiError, ApiResp, ProjectToken}
import domain.types.{LimsSpecimen, RcSpecimen}
import domain.services.{LimsSpecimenService, LimsSpecimenRemoteService}
import java.time.Instant

class LvToRcAggregator[F[_]: Sync: Functor: ConcurrentEffect: ContextShift[?[_]]]
(
  val apiAggregator: ApiAggregator[F],
  val localLimsSpecimenService: LimsSpecimenService[F],
  val remoteLimsSpecimenService: LimsSpecimenRemoteService[F]
) {

  private val lvRcSubjectIdFieldName = "lvrc_subject_id"

  private def record(forms: Option[String], patid: Option[String] = None, filter: Option[String] = None, fields: Option[String] = None): Chain[(String, String)] =
    Chain(
      "content" -> "record",
      "forms" -> forms.getOrElse("")
    ) ++ (patid match {
      case Some(a) => Chain("records" -> a)
      case None => Chain.empty[(String, String)]
    }) ++ (filter match {
      case Some(a) => Chain("filterLogic" -> a)
      case None => Chain.empty[(String, String)]
    }) ++ (fields match {
      case Some(a) => Chain("fields" -> a)
      case None => Chain.empty[(String, String)]
    })

  private val lvToRcKeyMap = Map[String, String](
      "LV ParticipantID" -> "spec_lv_participant_id",
      "EVENT" -> "spec_event",
      "SURGICAL_PATHOLOGY_NUMBER" -> "spec_surgical_path_nbr",
      "ANATOMIC_SITE" -> "spec_anatomic_site",
      "TISSUE_STATUS" -> "spec_tissue_status",
      "QUANTITY" -> "spec_quantity",
      "UNIT" -> "spec_unit",
      "STORAGE_STATUS" -> "spec_storage_status",
      "SAMPLE_TYPE" -> "spec_sample_type",
      "LOCATION" -> "spec_location"
    )

  private val sampleValueToRcSpecimen: Option[String] => Option[RcSpecimen] =
    sampleValue => {
      val kv =
        sampleValue
          .getOrElse("")
          .split(';')
          .map(_.split(':'))
          .filter(_.length == 2)
          .foldRight(Map[String, Any]())((a, m) =>
            m ++ (lvToRcKeyMap.find(_._1 == a(0)) match {
              case Some((k, k1)) => Map(k1 -> a(1))
              case _ => Map.empty[String, Any]
            })
          )

        CaseClassFromMap.mapToCaseClass[RcSpecimen](kv) match {
          case Right(a) => Some(a)
          case _ => None
        }
      }

  private def sampleValueToRcSpecimenPipeS(vals: List[LimsSpecimen]): Stream[F, List[RcSpecimen]] =
    Stream.emits(vals)
      .covary[F]
      .chunkN(100)
      .evalMap { s =>
        Sync[F].delay(
          s.map { t =>
            val recordId = if (t.USE_STUDYLINKID.getOrElse(0) == 1) t.STUDYLINKID else t.U_MRN

            sampleValueToRcSpecimen(t.SAMPLEVALUE)
              .getOrElse(RcSpecimen())
              .copy(RecordId = recordId, SpecDate = t.SAMPLE_COLLECTION_DATE, SpecModifyDate = t.MODIFYDATE, SpecSampleKey = t.SAMPLEKEY)
          }.filter(_.SpecModifyDate.isDefined).toList
        )
      }

  private def rcSpecimenToLimsSpecimen(batch: List[RcSpecimen], vals: List[LimsSpecimen]): List[LimsSpecimen] =
    vals.map { d =>
      val maybeWithStudyLinkId = batch.find(t => d.STUDYLINKID == t.RecordId && d.SAMPLEKEY == t.SpecSampleKey)
      val maybeWithMrn = batch.find(t => d.U_MRN == t.RecordId && d.SAMPLEKEY == t.SpecSampleKey)
      val e = maybeWithStudyLinkId <+> maybeWithMrn
      e match {
        case Some(a) => d.some
        case _ => None
      }
    }
    .filter(_.isDefined)
    .map(_.get)

  private def tryPersistListRcSpecimenImplPipeS(vals: List[(String, List[RcSpecimen])], token: Option[String], rcRecordId: Option[String] = None): Stream[F, (List[RcSpecimen], ApiResp)] =
    Stream.emits(vals)
      .covary[F]
      .flatMap[F, (List[RcSpecimen], ApiResp)] { case (_, samples) =>
        val body = record("research_specimens".some, rcRecordId) ++ Chain("token" -> token.getOrElse(""))

        apiAggregator
          .apiService
          .exportData[List[RcSpecimen]](body)
          .flatMap[F, (List[RcSpecimen], ApiResp)] { m =>
            m match {
              case Right(l) =>
                // Use the actual record_id stored in REDCap.
                val samplesWithRcRecordId = samples.map(a => a.copy(RecordId = rcRecordId))
                val samplesSorted = samplesWithRcRecordId.sortBy(_.SpecSampleKey.getOrElse(""))

                val sec1 = l.map(a => a.SpecSampleKey.getOrElse("")).sorted
                  .zipWithIndex

                val sec2 = sec1.reverse.headOption.getOrElse(("", -1))

                val ln = samplesSorted.foldLeft((sec2._2, List.empty[RcSpecimen])) { case (agg, s0) =>
                  val (inc, lst) = agg
                  sec1.find(a => a._1 == s0.SpecSampleKey.getOrElse("")) match {
                    case Some((_, idx)) =>
                      // Update
                      (inc, lst ++ List(s0.copy(
                        RedcapRepeatInstance = (idx + 1).some
                      )))
                    case None =>
                      // Insert
                      (inc + 1, lst ++List(s0.copy(
                        RedcapRepeatInstance = (inc + 2).some
                      )))
                  }
                }

                apiAggregator
                  .apiService
                  .importData[List[RcSpecimen]] (ln._2, Chain("content" -> "record") ++ Chain("token" -> token.getOrElse("")))
                  .flatMap[F, (List[RcSpecimen], ApiResp)](a => Stream.emit((ln._2, a)).covary[F])
              case Left(e) =>
                Stream.emit((samples, ApiError(Json.Null, e.show))).covary[F]
            }
        }
      }
  
  private def handlePersistResponse: List[LimsSpecimen] => ((List[RcSpecimen], ApiResp)) => Stream[F, Int] =
    vals => { 
      case (batch, res) => {
        val l = rcSpecimenToLimsSpecimen(batch, vals)
          .map(d => d.copy(
            processed = 1.some,
            date_processed = Instant.now.some,
            response = res match {
              case b: ApiOk => b.body.toString.some
              case _ => None
            },
            error = res match {
              case b: ApiError => b.error.toString.some
              case _ => None
            }      
          ))

        Stream
          .eval(localLimsSpecimenService.updateMany(l))
      }  
    }

  private def tryPersistRcSpecimenPipeS(vals: List[LimsSpecimen]): Option[ProjectToken] => Stream[F, Int] =
    maybeToken => {
      val token = maybeToken.getOrElse(ProjectToken()).token

      sampleValueToRcSpecimenPipeS(vals)
        .flatMap[F, Int] { s0 =>
          token.isDefined match {
            case a if a == true =>
              // Group by RecordId, fetch instruments for form 1 time.
              val s1 = s0.groupBy(_.RecordId.getOrElse(""))
                .mapValues(_.sortBy(_.SpecSampleKey.getOrElse("")))
                .toList

              tryFindRcSubject(token, vals, s1)
            case _ =>
              handlePersistResponse(vals)(s0, ApiError(Json.Null, "REDCAPID is missing."))
          }
        }
    }

  private def tryFindRcSubject(token: Option[String], vals: List[LimsSpecimen], s1: List[(String, List[RcSpecimen])]): Stream[F, Int] =
    Stream.emits(s1)
      .covary[F]
      .flatMap[F, Int] { case (recordId, samples) =>
        val body = record(None, None, s"${lvRcSubjectIdFieldName}='${recordId}'".some, "record_id".some) ++ Chain("token" -> token.getOrElse(""))
        apiAggregator
          .apiService
          .exportData[List[Json]](body)
          .flatMap[F, Int] { m =>
            m match {
              case Right(a) if a.nonEmpty =>
                val recordIdPath = root.record_id.string

                tryPersistListRcSpecimenImplPipeS(s1, token, recordIdPath.getOption(a.head))
                  .flatMap(handlePersistResponse(vals))
              case _ =>
                handlePersistResponse(vals)(samples, ApiError(Json.Null, "LVRC_SUBJECT_ID is missing."))
            }
          }
      }

  def fetchNext: Stream[F, Int] =
    Stream.eval(
      localLimsSpecimenService
        .getMaxDateProcessed
        .fold(_ => None, a => a)
      )
      .flatMap(remoteLimsSpecimenService.list(_))
      .chunkN(20)
      .map(_.toList)
      .flatMap[F, Int](x => Stream.eval(localLimsSpecimenService.insertMany(x)))
      
  def runUnprocessed: Stream[F, Int] =
    localLimsSpecimenService
      .listUnprocessed
      .chunkN(20)
      .map(_.toList)
      .flatMap[F, Int] { x =>
        val y = x.groupBy(_.REDCAPID.getOrElse(""))
          .mapValues(_.sortBy(_.SAMPLEKEY.getOrElse("")))
          .toList

        Stream.emits(y)
          .covary[F]
          .flatMap[F, Int] { case (key, vals) =>
            Stream.eval(apiAggregator.getProjectToken(key.some))
              .flatMap[F, Int](tryPersistRcSpecimenPipeS(vals))
        }
      }      
}

object LvToRcAggregator {
  def apply[F[_]: Sync: Functor: ConcurrentEffect: ContextShift[?[_]]]
  (
    apiAggregator: ApiAggregator[F],
    localLimsSpecimenService: LimsSpecimenService[F],
    remoteLimsSpecimenService: LimsSpecimenRemoteService[F]
  ): LvToRcAggregator[F] =
    new LvToRcAggregator(apiAggregator, localLimsSpecimenService, remoteLimsSpecimenService)
}
