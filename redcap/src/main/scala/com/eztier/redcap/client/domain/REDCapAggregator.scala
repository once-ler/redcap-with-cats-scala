package com.eztier.redcap.client
package domain

import cats.Applicative
import cats.data.Chain
import cats.effect.{Async, Concurrent}
import fs2.Stream

class REDCapAggregator[F[_]: Applicative: Async: Concurrent](metadataService: MetadataService[F], projectService: ProjectService[F], recordService: RecordService[F]) {
  def importMetadata(metadata: Option[Metadata] = None): Stream[F, ApiResp] =
    metadataService.importData(metadata)
  
  def exportMetadata(): Stream[F, Either[Chain[String], Metadata]] =
    metadataService.exportData()

  def importProject(project: Option[Project] = None): Stream[F, ApiResp] =
    projectService.importData(project)

  def exportProject(): Stream[F, Either[Chain[String], Project]] =
    projectService.exportData()

  def importRecord[A](records: A, options: Map[String, String]): Stream[F, ApiResp] =
    recordService.importData(records, options)

  def exportRecord[A](options: Map[String, String]): Stream[F, Either[Chain[String], A]] =
    recordService.exportData(options)
}

