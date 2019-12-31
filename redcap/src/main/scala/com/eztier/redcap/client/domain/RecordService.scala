package com.eztier.redcap.client
package domain

import algae.mtl.MonadLog
import cats.data.Chain
import cats.{Functor, Monad}
import fs2.Stream

class RecordService[F[_]: Functor: Monad : MonadLog[?[_], Chain[String]]](repository: RecordAlgebra[F]) {
  val logs = implicitly[MonadLog[F, Chain[String]]]

  def importData[A](records: A, options: Map[String, String]): Stream[F, ApiResp] =
    repository.importData[A](records, options)

  def exportData[A](options: Map[String, String]): Stream[F, Either[Chain[String], A]] =
    repository.exportData(options)
}

object RecordService {
  def apply[F[_]: Functor: Monad : MonadLog[?[_], Chain[String]]](repository: RecordAlgebra[F]): RecordService[F] =
    new RecordService[F](repository)
}
