package com.eztier
package redcap.client
package domain

import cats.data.Chain
import cats.{Functor, Monad}
import fs2.Stream
import io.circe.{Decoder, Encoder} // For Instant/String contramaps

import common.MonadLog

class ApiService[F[_]: Functor: Monad](repository: ApiAlgebra[F])(implicit ev: MonadLog[F, Chain[String]]) {
  
  val logs = implicitly[MonadLog[F, Chain[String]]]

  def importData[A](data: A, options: Chain[(String, String)])(implicit ev: Encoder[A]): Stream[F, ApiResp] =
    repository.importData[A](data, options)

  def exportData[A](options: Chain[(String, String)])(implicit ev: Decoder[A]): Stream[F, Either[Chain[String], A]] =
    repository.exportData(options)

  def createProject[A](data: A, odm: Option[String] = None)(implicit ev: Encoder[A]): Stream[F, ApiResp] =
    repository.createProject[A](data, odm)
}

object ApiService {
  def apply[F[_]: Functor: Monad](repository: ApiAlgebra[F])(implicit ev: MonadLog[F, Chain[String]]): ApiService[F] =
    new ApiService[F](repository)
}


