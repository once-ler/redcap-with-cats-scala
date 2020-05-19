package com.eztier.redcap.entity.limsmock.domain
package services

import cats.{Functor, Applicative}
import cats.data.EitherT
import cats.effect.Sync
import fs2.Stream
import java.time.Instant

import types._
import algebra._

class LimsSpecimenRemoteService[F[_]: Functor: Applicative: Sync](repository: LimsSpecimenRemoteAlgebra[F]) {

  def list(lastModifyDate: Option[Instant] = None): Stream[F, LimsSpecimen] =
    repository.list(lastModifyDate)
}

object LimsSpecimenRemoteService {
  def apply[F[_]: Functor: Applicative: Sync](repository: LimsSpecimenRemoteAlgebra[F]): LimsSpecimenRemoteService[F] =
    new LimsSpecimenRemoteService(repository)
}