package com.eztier.redcap.client
package domain

trait ProjectTokenAlgebra[F[_]] {
  def insertMany(a: List[ProjectToken]): F[Int]

  def list(): F[List[ProjectToken]]
}
