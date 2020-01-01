package com.eztier
package redcap.client
package infrastructure
package http

import algae.mtl.MonadLog
import cats.syntax.semigroup._
import cats.data.Chain
import cats.{Applicative, Functor}
import cats.effect.{ConcurrentEffect, ContextShift}
import org.http4s.{Charset, EntityBody, Header, Headers, HttpVersion, Method, Request, Uri, UrlForm}
import fs2.{Pipe, Stream}
import fs2.text.{utf8DecodeC, utf8Encode}
import org.http4s.client.blaze.BlazeClientBuilder
import io.circe._
import io.circe.generic.extras._ // For snakecase member names.
import io.circe.syntax._
import io.circe.parser._
import io.circe.optics.JsonPath._
import common.Util._
import domain._

class HttpInterpreter[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]: MonadLog[?[_], Chain[String]]]
  extends WithBlockingEcStream with ApiAlgebra[F] {

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

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

  def toApiResponseS: String => Stream[F, ApiResp] =
    in => {
      val json: Json = parse(in).getOrElse(Json.Null)

      val _error = root.error.string

      val error: Option[String] = _error.getOption(json)

      Stream.emit(error match {
        case Some(a) => ApiError(json, a)
        case None => ApiOk(json)
      })
  }

  def toMaybeTypeS[A](implicit ev: Decoder[A]): String => Stream[F, Either[Chain[String], A]] =
    in => {
      val json: Json = parse(in).getOrElse(Json.Null)

      val maybeA = json.as[A]

      Stream.emit(maybeA match {
        case Right(a) => Right(a)
        case Left(e) => Left(Chain.one(in))
      })
    }

  private def createRequest(formData: UrlForm) =
    Request[F](
      method = Method.POST,
      uri = Uri.unsafeFromString("https://www.w3schools.com/xml/tempconvert.asmx"),
      headers = moreHeaders,
      httpVersion = HttpVersion.`HTTP/1.1`
    ).withEntity(formData)(UrlForm.entityEncoder(Charset.`UTF-8`))

  override def importData[A](data: A, options: Chain[(String, String)])(implicit ev: Encoder[A]): Stream[F, ApiResp] = {

    val j: Json = data.asJson

    val formData = UrlForm.fromChain(defaultRequestBody |+| options) + ("data" -> j.noSpaces)

    // val a: Entity[F] = UrlForm.entityEncoder(charset).toEntity(formData)

    val request: Request[F] = createRequest(formData)

    clientBodyStream(request)
      .flatMap(toApiResponseS)
  }

  override def exportData[A](options: Chain[(String, String)])(implicit ev: Decoder[A]): Stream[F, Either[Chain[String], A]] = {
    // ("content" -> "record")
    val formData = UrlForm.fromChain(defaultRequestBody |+| options)

    val request: Request[F] = createRequest(formData)

    clientBodyStream(request)
      .flatMap(toMaybeTypeS)

  }
}

object HttpInterpreter {
  def apply[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]: MonadLog[?[_], Chain[String]]]: HttpInterpreter[F] = new HttpInterpreter[F]
}
