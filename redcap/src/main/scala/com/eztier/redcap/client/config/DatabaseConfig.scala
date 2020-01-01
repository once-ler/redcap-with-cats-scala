package com.eztier.redcap.client
package config

import cats.implicits._
import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import doobie.hikari.HikariTransactor
import io.chrisdavenport.log4cats.Logger
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import common._
import Util._
import CatsLogger._

case class DatabaseConnectionsConfig(poolSize: Int)
case class DatabaseConfig(
  url: String,
  driver: String,
  user: String,
  password: String,
  connections: DatabaseConnectionsConfig
)

object DatabaseConfig {
  def dbTransactor[F[_] : Async : ContextShift](
    dbc: DatabaseConfig,
    connEc: ExecutionContext,
    blocker: Blocker,
  ): Resource[F, HikariTransactor[F]] =
    HikariTransactor
      .newHikariTransactor[F](dbc.driver, dbc.url, dbc.user, dbc.password, connEc, blocker)

  // By default, flyway will look at ./my-project/src/main/resources/db/migration for versioned sql files.
  def initializeDb[F[_]](cfg: DatabaseConfig)(implicit S: Sync[F]): F[Unit] =
    S.delay {
      Try {
        val fw: Flyway = {
          Flyway
            .configure()
            .dataSource(cfg.url, cfg.user, cfg.password)
            .load()
        }
        fw.migrate()
      } match {
        case Failure(e) =>
          Logger[F].error(WrapThrowable(e).printStackTraceAsString)
        case Success(_) => ()
      }
    }.as(())
}
