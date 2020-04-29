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

  def createREDCapClientResource[F[_]: Async :ContextShift :ConcurrentEffect: Timer](targetConfig: String = "local") =
    for {
      implicit0(logs: MonadLog[F, Chain[String]]) <- Resource.liftF(MonadLog.createMonadLog[F, String])
      conf <- Resource.liftF(ConfigParser.decodePathF[F, AppConfig]("redcap"))
      dbConfig <- Resource.liftF(ConfigParser.decodePathF[F, DatabaseConfig](s"redcap.db.$targetConfig"))
      _ <- Resource.liftF(DatabaseConfig.initializeDb[F](dbConfig)) // Lifts an applicative into a resource. Resource[Tuple1, Nothing[Unit]]
      connEc <- ExecutionContexts.fixedThreadPool[F](dbConfig.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor[F](dbConfig, connEc, Blocker.liftExecutionContext(txnEc))
      tokenRepo = DoobieProjectTokenRepositoryInterpreter[F](xa)
      tokenService = ProjectTokenService(tokenRepo)
      apiRepoHttpConfig <- Resource.liftF(ConfigParser.decodePathF[F, HttpConfig](s"redcap.http.$targetConfig")) 
      apiRepo = HttpInterpreter[F](apiRepoHttpConfig)
      apiService = ApiService(apiRepo)
      apiAggregator = ApiAggregator(apiService, tokenService)
    } yield apiAggregator
}
