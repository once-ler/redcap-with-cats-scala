package com.eztier.labvantage.entity.limsmock
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

import com.eztier.redcap.entity.limsmock._
import domain.types._
import domain.algebra._
import com.eztier.common.MonadLog
import com.eztier.common._
import Util._
import CatsLogger._

private object LimsSpecimenSQL {
  /* We require conversion for date time */
  implicit val DateTimeMeta: Meta[Instant] =
    Meta[java.sql.Timestamp].imap(_.toInstant)(java.sql.Timestamp.from)

  def listSql: Query0[LimsSpecimen] = sql"""
    select SSTUDYID, REDCAPID, U_MRN, U_FIRSTNAME, U_LASTNAME, BIRTHDATE, STUDYLINKID, USE_STUDYLINKID, SAMPLEKEY, SAMPLEVALUE, SAMPLE_COLLECTION_DATE, CREATEDATE, MODIFYDATE
    FROM labvantage.limsspecimen
  """.query

  def insertManySql(a: List[LimsSpecimen]): ConnectionIO[Int] = {
    val stmt = """
      insert into labvantage.limsspecimen (SSTUDYID, REDCAPID, U_MRN, U_FIRSTNAME, U_LASTNAME, BIRTHDATE, STUDYLINKID, USE_STUDYLINKID, SAMPLEKEY, SAMPLEVALUE, SAMPLE_COLLECTION_DATE, CREATEDATE, MODIFYDATE)
      values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """

    Update[LimsSpecimen](stmt)
      .updateMany(a)
  }

  def findByIdSql(a: Option[String]): Query0[LimsSpecimen] =
    sql"""select SSTUDYID, REDCAPID, U_MRN, U_FIRSTNAME, U_LASTNAME, BIRTHDATE, STUDYLINKID, USE_STUDYLINKID, SAMPLEKEY, SAMPLEVALUE, SAMPLE_COLLECTION_DATE, CREATEDATE, MODIFYDATE
         from labvantage.limsspecimen where redcapid = ${a.getOrElse("")}""".query

}

class DoobieLimsSpecimenRepositoryInterpreter[F[_]: Bracket[?[_], Throwable]](val xa: Transactor[F])
  extends LimsSpecimenAlgebra[F] {
  import LimsSpecimenSQL._

  override def insertMany(a: List[LimsSpecimen]): F[Int] = insertManySql(a).transact(xa)

  override def list(): F[List[LimsSpecimen]] = listSql.to[List].transact(xa)

  override def findById(id: Option[String]): OptionT[F, Option[LimsSpecimen]] = {
    val fa = findByIdSql(id)
      .option
      .transact(xa)

    OptionT.liftF(fa)
  }
}

object DoobieLimsSpecimenRepositoryInterpreter {
  def apply[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F]): DoobieLimsSpecimenRepositoryInterpreter[F] =
    new DoobieLimsSpecimenRepositoryInterpreter(xa)
}