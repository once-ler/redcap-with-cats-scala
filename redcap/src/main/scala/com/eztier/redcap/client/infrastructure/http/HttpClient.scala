package com.eztier
package redcap.client
package infrastructure
package http

import algae.mtl.MonadLog
import cats.data.Chain
import cats.{Applicative, Functor}
import cats.effect.{ConcurrentEffect, ContextShift}
import org.http4s.{EntityBody, Header, Headers, Request}
import fs2.{Pipe, Stream}
import fs2.text.{utf8DecodeC, utf8Encode}
import org.http4s.client.blaze.BlazeClientBuilder
import io.circe._
import io.circe.parser._
import io.circe.optics.JsonPath._

import common.Util._
import domain._

abstract class HttpClient[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]: MonadLog[?[_], Chain[String]]]
  extends WithBlockingEcStream {

  val logs = implicitly[MonadLog[F, Chain[String]]]

  val headers = Headers.of(Header("Accept", "*/*"))

  def getBody(body: EntityBody[F]): F[Vector[Byte]] = body.compile.toVector

  def strBody(body: String): EntityBody[F] = fs2.Stream(body).through(utf8Encode)

  val moreHeaders = headers.put(Header("Content-Type", "application/x-www-form-urlencoded"))

  def defaultRequestBody: Chain[(String, String)] = Chain(
    "token" -> "",
    "format" -> "json",
    "type" -> "flat"
  )

  def clientBodyStream(request: Request[F]): Stream[F, String] =
    blockingEcStream.flatMap {
      ec =>
        for {
          client <- BlazeClientBuilder[F](ec).stream
          plainRequest <- Stream.eval[F, Request[F]](Applicative[F].pure[Request[F]](request))
          entityBody <- client.stream(plainRequest).flatMap(_.body.chunks).through(utf8DecodeC)
        } yield entityBody
    }.handleErrorWith {
      e =>
        val ex = WrapThrowable(e).printStackTraceAsString
        Stream.eval(logs.log(Chain.one(s"${ex}")))
          .flatMap(a => Stream.emit(ex))
    }

  def toApiResponse: String => Stream[F, ApiResp] =
    in => {
      val json: Json = parse(in).getOrElse(Json.Null)

      val _error = root.error.string

      val error: Option[String] = _error.getOption(json)

      Stream.emit(error match {
        case Some(a) => ApiError(json, a)
        case None => ApiOk(json)
      })
  }
}
