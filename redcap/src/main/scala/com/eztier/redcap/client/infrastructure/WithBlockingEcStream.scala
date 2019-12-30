package com.eztier.redcap.client
package infrastructure

import java.util.concurrent.Executors

import cats.effect.{ConcurrentEffect, Sync}
import fs2.Stream

import scala.concurrent.ExecutionContext

abstract class WithBlockingEcStream[F[_]: ConcurrentEffect] {
  // Don't block the main thread
  def blockingEcStream: Stream[F, ExecutionContext] =
    Stream.bracket(Sync[F].delay(Executors.newFixedThreadPool(4)))(pool =>
      Sync[F].delay(pool.shutdown()))
      .map(ExecutionContext.fromExecutorService)
}
