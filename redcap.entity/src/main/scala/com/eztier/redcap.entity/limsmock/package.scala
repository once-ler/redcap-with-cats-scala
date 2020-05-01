package com.eztier.redcap.entity

import cats.data.{Chain, Writer}
import cats.effect.{Async, Blocker, ConcurrentEffect, ContextShift, IO, Resource, Sync, Timer}
import doobie.util.ExecutionContexts
import io.circe.config.{parser => ConfigParser}
import java.util.concurrent.Executors

import com.eztier.labvantage.entity.limsmock.infrastructure.doobie.interpreters.DoobieLimsSpecimenRepositoryInterpreter
import com.eztier.redcap.entity.limsmock.domain.services.LimsSpecimenService

package object limsmock {

  import domain.aggregators.LvToRcAggregator
  import com.eztier.common.{MonadLog}
  import com.eztier.redcap.client._
  import com.eztier.redcap.entity.limsmock.config._

  def createLvToRcAggregatorResource[F[_]: Async :ContextShift :ConcurrentEffect: Timer] =
    for {
      implicit0(logs: MonadLog[F, Chain[String]]) <- Resource.liftF(MonadLog.createMonadLog[F, String])
      conf <- Resource.liftF(ConfigParser.decodePathF[F, AppConfig]("redcapEntity.limsmock"))
      _ <- Resource.liftF(DatabaseConfig.initializeDb[F](conf.db.local)) // Lifts an applicative into a resource. Resource[Tuple1, Nothing[Unit]]
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.local.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor[F](conf.db.local, connEc, Blocker.liftExecutionContext(txnEc))
      limsSpecimenRepo = DoobieLimsSpecimenRepositoryInterpreter[F](xa)
      limsSpecimenService = LimsSpecimenService(limsSpecimenRepo)
      rcResource <- for {
        localRcResource <- createREDCapClientResource[F]("local")
      } yield localRcResource
      rc2Aggregator = LvToRcAggregator(rcResource, limsSpecimenService)
    } yield rc2Aggregator

}
