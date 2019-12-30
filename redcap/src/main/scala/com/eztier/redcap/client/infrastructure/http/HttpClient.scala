package com.eztier
package redcap.client
package infrastructure
package http

import algae.mtl.MonadLog
import cats.data.Chain
import cats.{Applicative, Functor}
import cats.effect.{ConcurrentEffect, ContextShift}
import org.http4s.{EntityBody, Header, Headers, Request}
import fs2.Stream
import fs2.text.{utf8Encode, utf8DecodeC}
import org.http4s.client.blaze.BlazeClientBuilder
import common.Util._

abstract class HttpClient[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]: MonadLog[?[_], Chain[String]]]
  extends WithBlockingEcStream {

    val logs = implicitly[MonadLog[F, Chain[String]]]

    val headers = Headers.of(Header("Accept", "*/*"))

    def getBody(body: EntityBody[F]): F[Vector[Byte]] = body.compile.toVector

    def strBody(body: String): EntityBody[F] = fs2.Stream(body).through(utf8Encode)

    val moreHeaders = headers.put(Header("Content-Type", "application/x-www-form-urlencoded"))

    def defaultRequestBody = Map(
      "token" -> "",
      "format" -> "json",
      "type" -> "flat"
    )

  def createRequest: Request[F]

  def clientBodyStream: Stream[F, String] =
    blockingEcStream.flatMap {
      ec =>
        for {
          client <- BlazeClientBuilder[F](ec).stream
          plainRequest <- Stream.eval[F, Request[F]](Applicative[F].pure[Request[F]](createRequest))
          entityBody <- client.stream(plainRequest).flatMap(_.body.chunks).through(utf8DecodeC)
        } yield entityBody
    }.handleErrorWith {
      e =>
        val ex = WrapThrowable(e).printStackTraceAsString
        Stream.eval(logs.log(Chain.one(s"${ex}")))
          .flatMap(a => Stream.emit(ex))
    }

  }
