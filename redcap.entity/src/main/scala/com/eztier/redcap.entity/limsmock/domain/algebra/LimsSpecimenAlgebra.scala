package com.eztier.redcap.entity.limsmock.domain
package algebra

import cats.data.EitherT
import types._

trait LimsSpecimenAlgebra[F[_]] {
  def list(): F[List[LimsSpecimen]]

  def insertMany(recs: List[LimsSpecimen]): F[Int]

  def findById(id: Option[String]): EitherT[F, List[String], Option[LimsSpecimen]]
}
