package com.eztier.redcap.client
package infrastructure.doobie.interpreters

import cats.implicits._
import cats.Applicative
import cats.data.{Chain, OptionT}
import cats.effect.{Bracket, Sync}
import doobie.{ConnectionIO, Query0, Transactor, _}
import doobie.implicits._
import io.chrisdavenport.log4cats.Logger
import java.time.Instant
import scala.util.{Failure, Success, Try}

import domain._
import com.eztier.common.MonadLog
import com.eztier.common._
import Util._
import CatsLogger._

private object ProjectTokenSQL {

  def listSql: Query0[ProjectToken] = sql"""
    SELECT id, project_id, token, ts
    FROM redcap.project_token
  """.query

  def insertManySql(a: List[ProjectToken]): ConnectionIO[Int] = {
    val stmt = """
      insert into redcap.project_token (project_id, token)
      values (?, ?)
    """

    Update[ProjectToken](stmt)
      .updateMany(a)
  }

}

class DoobieProjectTokenRepositoryInterpreter[F[_]: Bracket[?[_], Throwable]](val xa: Transactor[F])
  extends ProjectTokenAlgebra[F] {
  import ProjectTokenSQL._

  override def insertMany(a: List[ProjectToken]): F[Int] = insertManySql(a).transact(xa)

  override def list(): F[List[ProjectToken]] = listSql.to[List].transact(xa)
}

object DoobieProjectTokenRepositoryInterpreter {
  def apply[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F]): DoobieProjectTokenRepositoryInterpreter[F] =
    new DoobieProjectTokenRepositoryInterpreter(xa)
}
