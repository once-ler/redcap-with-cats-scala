package com.eztier.redcap.client
package infrastructure
package http

class HttpClient[F[_]: Functor: ConcurrentEffect: ContextShift[?[_]]]
  extends WithBlockingEcStream {
    val headers = Headers.of(Header("Accept", "*/*"))

    def getBody(body: EntityBody[F]): F[Vector[Byte]] = body.compile.toVector

    def strBody(body: String): EntityBody[F] = fs2.Stream(body).through(utf8Encode)

    val moreHeaders = headers.put(Header(`Content-Type`(MediaType.`application/x-www-form-urlencoded`)))

  }
