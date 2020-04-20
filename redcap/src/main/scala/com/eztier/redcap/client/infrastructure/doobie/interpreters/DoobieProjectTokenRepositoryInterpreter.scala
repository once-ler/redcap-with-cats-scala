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
  /* We require conversion for date time */
  implicit val DateTimeMeta: Meta[Instant] =
    Meta[java.sql.Timestamp].imap(_.toInstant)(java.sql.Timestamp.from)

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

  def findByIdSql(a: Option[String]): Query0[ProjectToken] =
    sql"id, project_id, token, ts from redcap.project_token where project_id = ${a.getOrElse("")}".query

}

class DoobieProjectTokenRepositoryInterpreter[F[_]: Bracket[?[_], Throwable]](val xa: Transactor[F])
  extends ProjectTokenAlgebra[F] {
  import ProjectTokenSQL._

  override def insertMany(a: List[ProjectToken]): F[Int] = insertManySql(a).transact(xa)

  override def list(): F[List[ProjectToken]] = listSql.to[List].transact(xa)

  override def findById(id: Option[String]): OptionT[F, Option[ProjectToken]] = {
    val fa = findByIdSql(id)
      .option
      .transact(xa)

    OptionT.liftF(fa)
  }
}

object DoobieProjectTokenRepositoryInterpreter {
  def apply[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F]): DoobieProjectTokenRepositoryInterpreter[F] =
    new DoobieProjectTokenRepositoryInterpreter(xa)
}
