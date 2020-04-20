package com.eztier.redcap.client
package domain

import cats.data.OptionT

trait ProjectTokenAlgebra[F[_]] {
  def insertMany(a: List[ProjectToken]): F[Int]

  def list(): F[List[ProjectToken]]

  def findById(id: Option[String]): OptionT[F, Option[ProjectToken]]
}
