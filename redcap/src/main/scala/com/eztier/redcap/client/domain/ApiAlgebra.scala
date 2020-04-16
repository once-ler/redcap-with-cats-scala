package com.eztier.redcap.client
package domain

import cats.data.Chain
import fs2.Stream
import io.circe.{Decoder, Encoder}

trait ApiAlgebra[F[_]] {
  def importData[A](data: A, options: Chain[(String, String)])(implicit ev: Encoder[A]): Stream[F, ApiResp]
  def exportData[A](options: Chain[(String, String)])(implicit ev: Decoder[A]): Stream[F, Either[Chain[String], A]]
  def createProject[A](data: A, odm: Option[String] = None): Stream[F, ApiResp]
}
