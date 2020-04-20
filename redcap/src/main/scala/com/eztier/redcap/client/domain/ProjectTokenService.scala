package com.eztier.redcap.client
package domain

import cats.{Functor, Applicative}
import cats.data.EitherT
import cats.effect.Sync

class ProjectTokenService[F[_]: Functor: Applicative: Sync](repository: ProjectTokenAlgebra[F]) {

  def list(): F[List[ProjectToken]] =
    repository.list()

  def insertMany(recs: List[ProjectToken]): F[Int] =
    repository.insertMany(recs)

  def findById(id: Option[String]): EitherT[F, List[String], Option[ProjectToken]] =
    repository.findById(id)
      .toRight(List(s"Failed to find project token."))
}

object ProjectTokenService {
  def apply[F[_]: Functor: Applicative: Sync](repository: ProjectTokenAlgebra[F]): ProjectTokenService[F] =
    new ProjectTokenService(repository)
}
