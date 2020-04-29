package com.eztier
package redcap
package entity

import cats.data.{Chain, Writer}
import cats.effect.{Async, Blocker, ConcurrentEffect, ContextShift, IO, Resource, Sync, Timer}
import doobie.util.ExecutionContexts
import io.circe.config.{parser => ConfigParser}
import java.util.concurrent.Executors

import domain.aggregators.Rc2Aggregator

package object randmock {
  import com.eztier.common.{MonadLog, _}
  import com.eztier.redcap.entity._
  import domain._
  import config._
  import infrastructure.doobie._
  import infrastructure.doobie._
  import infrastructure.http._
  import infrastructure.doobie.interpreters._

  def createRc2AggregatorResource[F[_]: Async :ContextShift :ConcurrentEffect: Timer] =
    for {
      implicit0(logs: MonadLog[F, Chain[String]]) <- Resource.liftF(MonadLog.createMonadLog[F, String])
      conf <- Resource.liftF(ConfigParser.decodePathF[F, AppConfig]("redcapEntity.randmock"))
      _ <- Resource.liftF(DatabaseConfig.initializeDb[F](conf.db.local))
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.local.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor[F](conf.db.local, connEc, Blocker.liftExecutionContext(txnEc))
      tokenRepo = DoobieProjectTokenRepositoryInterpreter[F](xa)
      tokenService = ProjectTokenService(tokenRepo)
      localApiRepo = HttpInterpreter[F](conf.http.local)
      localApiService = ApiService(localApiRepo)
      localApiAggregator = ApiAggregator(apiService, tokenService)
      remoteApiRepo = HttpInterpreter[F](conf.http.remote)
      remoteApiService = ApiService(remoteApiRepo)
      remoteApiAggregator = ApiAggregator(remoteApiService, tokenService)
      rc2Aggregator = Rc2Aggregator(localApiAggregator, remoteApiAggregator, conf.http.local.form, conf.http.remote.form)
    } yield rc2Aggregator
}
