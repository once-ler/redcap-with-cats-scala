package com.eztier.redcap.entity.limsmock.domain
package algebra

import cats.data.{EitherT, OptionT}
import java.time.Instant
import types._

trait LimsSpecimenAlgebra[F[_]] {
  // def list(lastModifyDate: Option[Instant] = None): F[List[LimsSpecimen]]

  def insertMany(recs: List[LimsSpecimen]): F[Int]

  def findById(id: Option[String]): OptionT[F, Option[LimsSpecimen]]

  def getMaxDateProcessed: OptionT[F, Option[Instant]]
}
