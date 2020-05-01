package com.eztier.redcap.entity

import cats.effect._
import cats.implicits._
import fs2.Stream

import randmock.domain.aggregators.Rc2Aggregator
import randmock.domain.types.RcLocalRandomization
import randmock._

import scala.concurrent.duration._
object App extends IOApp {

  def createAggregators[F[_]: ContextShift: ConcurrentEffect: Timer]: IO[Unit] = {
    val r: Resource[IO, (Rc2Aggregator[IO])] = for {
      r0 <- createRc2AggregatorResource[IO]
    } yield (r0)

    r.use {
      case (src) =>

        src.fetch[RcLocalRandomization]
          .compile
          .drain
          .unsafeRunSync()

        println(Stream.eval(src.showLog).compile.toList.unsafeRunSync())

        IO.unit
    }
  }

  override def run(args: List[String]): IO[ExitCode] =
    IO.delay(
      createAggregators[IO].unsafeRunSync()
    ).as(ExitCode.Success)
}
