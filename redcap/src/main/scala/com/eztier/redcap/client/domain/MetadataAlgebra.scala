package com.eztier.redcap.client
package domain

trait MetadataAlgebra[F[_]] {
  def importData(metadata: Option[Metadata] = None): Stream[F, ApiResp]
  def exportData(): Stream[F, Either[Chain[String], Metadata]]
}
