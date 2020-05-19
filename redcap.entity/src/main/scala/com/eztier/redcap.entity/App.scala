package com.eztier.redcap.entity

import cats.effect._
import cats.implicits._
import fs2.Stream
import scala.concurrent.duration._

import randmock.domain.aggregators.Rc2Aggregator
import randmock.domain.types.RcLocalRandomization
import randmock._

import limsmock._

import scala.concurrent.duration._
object App2 extends IOApp {

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

object App extends IOApp {

  private def pause[F[_]: Timer](d: FiniteDuration) = Stream.emit(1).covary[F].delayBy(d)

  private def repeat(io : IO[Unit]): IO[Nothing] =
    IO.suspend(
      io
        *> IO.delay(pause[IO](10 seconds).compile.drain.unsafeRunSync())
        *> repeat(io)
    )

  def createAggregators[F[_]: ContextShift: ConcurrentEffect: Timer]: IO[Unit] = {
    val r = for {
      rc <- createLvToRcAggregatorResource[IO]
    } yield rc

    r.use { case lvToRcAggregator =>

      val io = lvToRcAggregator
        .runUnprocessed
        .compile
        .drain

      val io2 = lvToRcAggregator
        .fetchNext
        .compile
        .drain

      repeat(io).unsafeRunSync()
    }
  }
     
  override def run(args: List[String]): IO[ExitCode] =
    IO.delay(
      createAggregators[IO].unsafeRunSync()
    ).as(ExitCode.Success)

}
