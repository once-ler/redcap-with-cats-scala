package com.eztier
package common

import cats.implicits._
import cats.{Functor, Monad}
import cats.data.Chain
import cats.effect.Sync
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

object CatsLogger {
  implicit def unsafeLogger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

  implicit class CombineMonadLog[F[_]: Monad :Functor, L](lhs: MonadLog[F, Chain[L]]) {
    def combineK(rhs: MonadLog[F, Chain[L]]) = {
      for {
        l0 <- lhs.get
        l1 <- rhs.get
        k = l0 <+> l1
      } yield k
    }
  }
}
