package com.eztier.redcap.entity.limsmock.domain.aggregators

import cats.implicits._
import cats.Functor
import cats.effect.{ConcurrentEffect, ContextShift}
import com.eztier.redcap.client.domain.ApiAggregator

class LvToRcAggregator[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
(
  apiAggregator: ApiAggregator[F]
) {

}

object LvToRcAggregator {
  def apply[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
  (
    apiAggregator: ApiAggregator[F]
  ): LvToRcAggregator[F] =
    new LvToRcAggregator(apiAggregator)
}
