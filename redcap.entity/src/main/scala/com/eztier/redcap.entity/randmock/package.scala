package com.eztier
package redcap
package entity

import cats.data.{Chain, Writer}
import cats.effect.{Async, Blocker, ConcurrentEffect, ContextShift, IO, Resource, Sync, Timer}
import doobie.util.ExecutionContexts
import io.circe.config.{parser => ConfigParser}
import java.util.concurrent.Executors

import com.eztier.redcap.entity.randmock.domain.aggregators.Rc2Aggregator

package object randmock {
  import com.eztier.common.{MonadLog, _}
  import com.eztier.redcap.client._
  import domain._
  import infrastructure.http._
  import infrastructure.doobie.interpreters._
  import com.eztier.redcap.entity.randmock.config._

  def createRc2AggregatorResource[F[_]: Async :ContextShift :ConcurrentEffect: Timer] =
    for {
      implicit0(logs: MonadLog[F, Chain[String]]) <- Resource.liftF(MonadLog.createMonadLog[F, String])
      conf <- Resource.liftF(ConfigParser.decodePathF[F, AppConfig]("redcapEntity.randmock"))
      rcResources <- for {
          localRcResource <- createREDCapClientResource[F]("local")
          remoteRcResource <- createREDCapClientResource[F]("remote")
        } yield (localRcResource, remoteRcResource)
      rc2Aggregator = Rc2Aggregator(rcResources._1, rcResources._2, conf.http.local.form, conf.http.remote.form)
    } yield rc2Aggregator
}
