package com.eztier
package redcap.client
package infrastructure
package http

import cats.syntax.semigroup._
import cats.data.Chain
import cats.{Applicative, Functor}
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Sync}
import cats.implicits._
import fs2.{Pipe, Stream}
import fs2.text.{utf8Decode, utf8DecodeC, utf8Encode}
import io.circe._
import io.circe.generic.extras._
import io.circe.syntax._
import io.circe.parser._
import io.circe.optics.JsonPath._
import java.nio.file.Paths
import java.util.concurrent.Executors

import org.http4s._
import org.http4s.client.blaze.BlazeClientBuilder
import common.{MonadLog, _}
import common.Util._
import domain._
import com.eztier.redcap.client.config.HttpConfig

import scala.concurrent.ExecutionContext

class HttpInterpreter[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
  (conf: HttpConfig)(implicit logs: MonadLog[F, Chain[String]])
  extends ApiAlgebra[F] {

  // implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  private val pool = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))
  private val client = BlazeClientBuilder(pool)
  private val blocker = Blocker.liftExecutionContext(pool)

  val headers = Headers.of(Header("Accept", "*/*"))

  def getBody(body: EntityBody[F]): F[Vector[Byte]] = body.compile.toVector

  def strBody(body: String): EntityBody[F] = fs2.Stream(body).through(utf8Encode)

  // val moreHeaders = headers.put(Header("Content-Type", "application/x-www-form-urlencoded"))

  def defaultRequestBody: Chain[(String, String)] = Chain(
    "token" -> conf.token.getOrElse(""),
    "format" -> "json",
    "type" -> "flat"
  )

  def clientBodyStream(request: Request[F]): Stream[F, String] =
    for {
      client <- client.stream
      plainRequest <- Stream.eval[F, Request[F]](Applicative[F].pure[Request[F]](request))
      entityBody <- client.stream(plainRequest).flatMap(_.body.chunks).through(utf8DecodeC)
        .handleErrorWith { e =>
          val ex = WrapThrowable(e).printStackTraceAsString
          Stream.eval(logs.log(Chain.one(s"${ex}")))
            .flatMap(a => Stream.emit(ex))
        }
    } yield entityBody

  def toApiResponseS: String => Stream[F, ApiResp] =
    in => {
      val json: Json = parse(in).getOrElse(Json.fromString(in))

      val _error = root.error.string

      val error: Option[String] = _error.getOption(json)

      Stream.emit(error match {
        case Some(a) => ApiError(json, a)
        case None => ApiOk(json)
      })
  }

  def toMaybeTypeS[A](implicit ev: Decoder[A]): String => Stream[F, Either[Chain[String], A]] =
    in => {
      val json: Json = parse(in).getOrElse(Json.fromString(in))

      val maybeA = json.as[A]

      Stream.emit(maybeA match {
        case Right(a) => Right(a)
        case Left(e) => Left(Chain.one(in))
      })
    }

  def readAllFromFile(path: String, bufferSize: Int = 8192): Stream[F, String] =
    for {

      str <- fs2.io.file.readAll[F](Paths.get(path), blocker, bufferSize)
        .through(utf8Decode)
        .handleErrorWith { e =>
          val ex = WrapThrowable(e).printStackTraceAsString
          Stream.eval(logs.log(Chain.one(s"${ex}")))
            .flatMap(a => Stream.emit(ex))
        }
    } yield str

  private def createRequest(formData: UrlForm, inHeaders: Headers = Headers.empty) =
    Request[F](
      method = Method.POST,
      uri = Uri.unsafeFromString(conf.url),
      headers = headers ++ inHeaders ++ (inHeaders.exists(_.name == "Content-Type") match {
        case true => Headers.empty
        case _ => Headers.of(Header("Content-Type", "application/x-www-form-urlencoded"))
      }),
      httpVersion = HttpVersion.`HTTP/1.1`
    ).withEntity(formData)(UrlForm.entityEncoder(Charset.`UTF-8`))

  override def importData[A](data: A, options: Chain[(String, String)], headers: Headers = Headers.empty)(implicit ev: Encoder[A]): Stream[F, ApiResp] = {

    val j: Json = data.asJson

    val formData = UrlForm.fromChain(defaultRequestBody <+> options) + ("data" -> j.noSpaces)

    // val a: Entity[F] = UrlForm.entityEncoder(charset).toEntity(formData)

    val request: Request[F] = createRequest(formData, headers)

    clientBodyStream(request)
      .flatMap(toApiResponseS)
  }

  override def exportData[A](options: Chain[(String, String)])(implicit ev: Decoder[A]): Stream[F, Either[Chain[String], A]] = {
    // ("content" -> "record")
    val formData = UrlForm.fromChain(defaultRequestBody <+> options)

    val request: Request[F] = createRequest(formData)

    clientBodyStream(request)
      .flatMap(toMaybeTypeS)

  }

  override def createProject[A](data: A, odm: Option[String])(implicit ev: Encoder[A]): Stream[F, ApiResp] = {
    /*
    val maybeOdm: Chain[(String, String)] = odm match {
      case Some(a) => Chain.one("odm" -> a)
      case None => Chain.empty
    }
    
    val formData = UrlForm.fromChain(Chain("data" -> data.asJson.noSpaces) |+| maybeOdm)

    val request: Request[F] = createRequest(formData)

    clientBodyStream(request)
      .flatMap(toApiResponseS)
  */
    ???
  }

  override def showLog: F[String] =
    for {
      l <- logs.get
    } yield l.show

  override def showConf: F[HttpConfig] =
    Sync[F].delay(conf)

}

object HttpInterpreter {
  def apply[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]: MonadLog[?[_], Chain[String]]](conf: HttpConfig): HttpInterpreter[F] = new HttpInterpreter[F](conf)
}
