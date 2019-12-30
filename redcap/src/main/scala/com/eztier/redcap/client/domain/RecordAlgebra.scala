package com.eztier.redcap.client
package domain

import cats.data.Chain
import fs2.Stream

trait RecordAlgebra[F[_]] {
  def importData[A](options: Map[String, String]): Stream[F, ApiResp]
  def exportData[A](options: Map[String, String]): Stream[F, Either[Chain[String], A]]
}
