package com.eztier.redcap.client
package infrastructure.http

import algae.mtl.MonadLog
import cats.Functor
import cats.data.Chain
import cats.effect.{ConcurrentEffect, ContextShift}
import org.http4s.Request

class HttpProjectInterpreter[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]: MonadLog[?[_], Chain[String]]] extends HttpClient[F] {

}
