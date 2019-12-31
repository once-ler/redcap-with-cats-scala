package com.eztier.redcap.client
package domain

import cats.data.Chain
import fs2.Stream
import io.circe.Encoder

trait RecordAlgebra[F[_]] {
  def importData[A](records: A, options: Map[String, String])(implicit ev: Encoder[A]): Stream[F, ApiResp]
  def exportData[A](options: Map[String, String]): Stream[F, Either[Chain[String], A]]
}
