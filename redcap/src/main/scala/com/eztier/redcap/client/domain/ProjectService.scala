package com.eztier.redcap.client
package domain

import algae.mtl.MonadLog
import cats.data.Chain
import cats.{Functor, Monad}
import fs2.Stream

class ProjectService[F[_]: Functor: Monad : MonadLog[?[_], Chain[String]]](repository: ProjectAlgebra[F]) {
  val logs = implicitly[MonadLog[F, Chain[String]]]

  def importData(project: Option[Project] = None): Stream[F, ApiResp] =
    repository.importData(project)

  def exportData(): Stream[F, Either[Chain[String], Project]] =
    repository.exportData()
}

object ProjectService {
  def apply[F[_]: Functor: Monad : MonadLog[?[_], Chain[String]]](repository: ProjectAlgebra[F]): ProjectService[F] =
    new ProjectService[F](repository)
}
