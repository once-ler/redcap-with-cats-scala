package com.eztier.redcap.client
package domain

import algae.mtl.MonadLog
import cats.data.Chain
import cats.{Functor, Monad}
import fs2.Stream

class MetadataService[F[_]: Functor: Monad : MonadLog[?[_], Chain[String]]](repository: MetadataAlgebra[F]) {
  val logs = implicitly[MonadLog[F, Chain[String]]]

  def importData(metadata: Option[Metadata] = None): Stream[F, ApiResp] =
    repository.importData(metadata)

  def exportData(): Stream[F, Either[Chain[String], Metadata]] =
    repository.exportData()
}

object MetadataService {
  def apply[F[_]: Functor: Monad : MonadLog[?[_], Chain[String]]](repository: MetadataAlgebra[F]): MetadataService[F] =
    new MetadataService[F](repository)
}
