package com.eztier.redcap.entity.limsmock.domain
package services

import cats.{Functor, Applicative}
import cats.data.EitherT
import cats.effect.Sync

import types._
import algebra._

class LimsSpecimenService[F[_]: Functor: Applicative: Sync](repository: LimsSpecimenAlgebra[F]) {

  def list(): F[List[LimsSpecimen]] =
    repository.list()

  def insertMany(recs: List[LimsSpecimen]): F[Int] =
    repository.insertMany(recs)

  def findById(id: Option[String]): EitherT[F, List[String], Option[LimsSpecimen]] =
    repository.findById(id)
      .toRight(List(s"Failed to find project token."))
}

object LimsSpecimenService {
  def apply[F[_]: Functor: Applicative: Sync](repository: LimsSpecimenAlgebra[F]): LimsSpecimenService[F] =
    new LimsSpecimenService(repository)
}