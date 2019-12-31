package com.eztier.redcap.client
package infrastructure.http

import algae.mtl.MonadLog
import cats.Functor
import cats.data.Chain
import cats.effect.{ConcurrentEffect, ContextShift}
import org.http4s.{Charset, Entity, HttpVersion, Method, Request, Uri, UrlForm}
import domain._
import io.circe.{Encoder, Json}
import io.circe.syntax._
import fs2.Stream

class HttpRecordInterpreter[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]: MonadLog[?[_], Chain[String]]]
  extends HttpClient[F] with RecordAlgebra[F] {

  override def importData[A](records: A, options: Map[String, String])(implicit ev: Encoder[A]): Stream[F, ApiResp] = {
    val j: Json = records.asJson

    val formData = UrlForm.fromChain(defaultRequestBody) + ("data" -> j.noSpaces)

    // val a: Entity[F] = UrlForm.entityEncoder(charset).toEntity(formData)

    val request: Request[F] = Request[F](
      method = Method.POST,
      uri = Uri.unsafeFromString("https://www.w3schools.com/xml/tempconvert.asmx"),
      headers = moreHeaders,
      httpVersion = HttpVersion.`HTTP/1.1`
    ).withEntity(formData)(UrlForm.entityEncoder(Charset.`UTF-8`))

    clientBodyStream(request)
      .flatMap(toApiResponse)
  }

  override def exportData[A](options: Map[String, String]): fs2.Stream[F, Either[Chain[String], A]] = ???

}
