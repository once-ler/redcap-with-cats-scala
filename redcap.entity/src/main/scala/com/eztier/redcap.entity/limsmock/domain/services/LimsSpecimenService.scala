package com.eztier.redcap.entity.limsmock.domain
package services

import cats.{Functor, Applicative}
import cats.data.EitherT
import cats.effect.Sync
import java.time.Instant

import types._
import algebra._

class LimsSpecimenService[F[_]: Functor: Applicative: Sync](repository: LimsSpecimenAlgebra[F]) {

  def list(lastModifyDate: Option[Instant] = None): F[List[LimsSpecimen]] =
    repository.list(lastModifyDate)

  def insertMany(recs: List[LimsSpecimen]): F[Int] =
    repository.insertMany(recs)

  def findById(id: Option[String]): EitherT[F, List[String], Option[LimsSpecimen]] =
    repository.findById(id)
      .toRight(List(s"Failed to find project token."))

  def getMaxDateProcessed: EitherT[F, List[String], Option[Instant]] =
    repository.getMaxDateProcessed
      .toRight(List(s"Failed to find max processed date."))
}

object LimsSpecimenService {
  def apply[F[_]: Functor: Applicative: Sync](repository: LimsSpecimenAlgebra[F]): LimsSpecimenService[F] =
    new LimsSpecimenService(repository)
}