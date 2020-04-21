package com.eztier
package redcap

import cats.data.{Chain, Writer}
import cats.effect.{Async, Blocker, ConcurrentEffect, ContextShift, IO, Resource, Sync, Timer}
import doobie.util.ExecutionContexts
import io.circe.config.{parser => ConfigParser}
import java.util.concurrent.Executors

import common.{MonadLog, _}

package object client {
  import domain._
  import config._
  // import infrastructure.doobie._
  import infrastructure.http._
  import infrastructure.doobie.interpreters._

  def createREDCapClientResource[F[_]: Async :ContextShift :ConcurrentEffect: Timer] =
    for {
      implicit0(logs: MonadLog[F, Chain[String]]) <- Resource.liftF(MonadLog.createMonadLog[F, String])
      conf <- Resource.liftF(ConfigParser.decodePathF[F, AppConfig]("redcap"))
      _ <- Resource.liftF(DatabaseConfig.initializeDb[F](conf.db.local)) // Lifts an applicative into a resource. Resource[Tuple1, Nothing[Unit]]
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.local.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor[F](conf.db.local, connEc, Blocker.liftExecutionContext(txnEc))
      tokenRepo = DoobieProjectTokenRepositoryInterpreter[F](xa)
      tokenService = ProjectTokenService(tokenRepo)
      apiRepo = HttpInterpreter[F](conf.http.local)
      apiService = ApiService(apiRepo)
      apiAggregator = ApiAggregator(apiService, tokenService)
    } yield apiAggregator
}
