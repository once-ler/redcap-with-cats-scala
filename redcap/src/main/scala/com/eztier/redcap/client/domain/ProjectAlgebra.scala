package com.eztier.redcap.client
package domain

import cats.data.Chain
import fs2.Stream

trait ProjectAlgebra[F[_]] {
  def importData(project: Option[Project] = None): Stream[F, ApiResp]
  def exportData(): Stream[F, Either[Chain[String], Project]]
}
