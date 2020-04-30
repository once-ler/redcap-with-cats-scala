package com.eztier.redcap.entity;

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
