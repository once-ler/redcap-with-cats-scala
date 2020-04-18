package com.eztier.redcap.client
package domain

import cats.data.Chain
import fs2.Stream
import io.circe.{Decoder, Encoder}
import org.http4s.Headers

import config.HttpConfig

trait ApiAlgebra[F[_]] {
  def importData[A](data: A, options: Chain[(String, String)], headers: Headers = Headers.empty)(implicit ev: Encoder[A]): Stream[F, ApiResp]
  def exportData[A](options: Chain[(String, String)])(implicit ev: Decoder[A]): Stream[F, Either[Chain[String], A]]
  def createProject[A](data: A, odm: Option[String] = None)(implicit ev: Encoder[A]): Stream[F, ApiResp]
  def readAllFromFile(path: String, bufferSize: Int = 8192): Stream[F, String]
  def showLog: F[String]
  def showConf: F[HttpConfig]
}
