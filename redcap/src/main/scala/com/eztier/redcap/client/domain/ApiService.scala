package com.eztier.redcap.client
package domain

import algae.mtl.MonadLog
import cats.data.Chain
import cats.{Functor, Monad}
import fs2.Stream

import io.circe.generic.auto._
import domain._ // For Instant/String contramaps

class ApiService[F[_]: Functor: Monad : MonadLog[?[_], Chain[String]]](repository: ApiAlgebra[F]) {
  val logs = implicitly[MonadLog[F, Chain[String]]]

  def importData[A](data: A, options: Chain[(String, String)]): Stream[F, ApiResp] =
    repository.importData[A](data, options)

  def exportData[A](options: Chain[(String, String)]): Stream[F, Either[Chain[String], A]] =
    repository.exportData(options)
}

object ApiService {
  def apply[F[_]: Functor: Monad : MonadLog[?[_], Chain[String]]](repository: ApiAlgebra[F]): ApiService[F] =
    new ApiService[F](repository)
}


