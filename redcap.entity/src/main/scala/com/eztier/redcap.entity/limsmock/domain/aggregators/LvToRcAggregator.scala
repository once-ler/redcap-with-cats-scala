package com.eztier.redcap.entity.limsmock.domain.aggregators

import cats.implicits._
import cats.Functor
import cats.effect.{ConcurrentEffect, ContextShift}
import com.eztier.redcap.client.domain.ApiAggregator
import com.eztier.redcap.entity.limsmock.domain.services.LimsSpecimenService

class LvToRcAggregator[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
(
  val apiAggregator: ApiAggregator[F],
  val limsSpecimenService: LimsSpecimenService[F]
) {

}

object LvToRcAggregator {
  def apply[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
  (
    apiAggregator: ApiAggregator[F],
    limsSpecimenService: LimsSpecimenService[F]
  ): LvToRcAggregator[F] =
    new LvToRcAggregator(apiAggregator, limsSpecimenService)
}
