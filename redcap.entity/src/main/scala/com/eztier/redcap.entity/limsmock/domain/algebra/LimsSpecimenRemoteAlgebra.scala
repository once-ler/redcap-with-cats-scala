package com.eztier.redcap.entity.limsmock.domain
package algebra

import cats.data.{EitherT, OptionT}
import fs2.Stream
import java.time.Instant
import types._

trait LimsSpecimenRemoteAlgebra[F[_]] {
  def list(lastModifyDate: Option[Instant] = None): Stream[F, LimsSpecimen]
}
