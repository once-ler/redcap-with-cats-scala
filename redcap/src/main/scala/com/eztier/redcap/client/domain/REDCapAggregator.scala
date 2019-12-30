package com.eztier.redcap.client
package domain

class REDCapAggregator[F[_]: Applicative: Async: Concurrent](metadataService: MetadataService, projectService: ProjectService, recordService: RecordService) {
  def importMetadata(metadata: Option[Metadata] = None): Stream[F, ApiResp] =
    metadataService.importData(metadata)
  
  def exportMetadata(): Stream[F, Either[Chain[String], Metadata]] =
    metadataService.exportData()

  def importProject(project: Option[Project] = None): Stream[F, ApiResp] =
    projectService.importData(project)

  def exportProject(): Stream[F, Either[Chain[String], Project]] =
    projectService.exportData()

  def importRecord[A](options: Map[String, String]): Stream[F, ApiResp] =
    recordService.importData(options)

  def exportRecord[A](options: Map[String, String]): Stream[F, Either[Chain[String], A]] =
    recordService.exportData(options)
}

