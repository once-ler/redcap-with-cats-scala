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

  val listFragment = fr"""
    select id, SSTUDYID, REDCAPID, U_MRN, U_FIRSTNAME, U_LASTNAME, BIRTHDATE, STUDYLINKID, USE_STUDYLINKID, SAMPLEKEY, SAMPLEVALUE, SAMPLE_COLLECTION_DATE, CREATEDATE, MODIFYDATE
    from labvantage.limsspecimen
  """

  val listCriteriaFragment = fr" where processed = 0"

  def listSql: Query0[LimsSpecimen] =
    (listFragment ++ listCriteriaFragment).query

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

  def getMaxDateProcessedSql: Query0[Option[java.time.Instant]] =
    sql"""select max(MODIFYDATE) MODIFYDATE from labvantage.limsspecimen
      """.query
     
}

class DoobieLimsSpecimenRepositoryInterpreter[F[_]: Bracket[?[_], Throwable]](val xa: Transactor[F])(implicit logs: MonadLog[F, Chain[String]])
  extends LimsSpecimenAlgebra[F] {
  import LimsSpecimenSQL._

  override def insertMany(a: List[LimsSpecimen]): F[Int] = insertManySql(a).transact(xa)

  override def listUnprocessed: F[List[LimsSpecimen]] = listSql.to[List].transact(xa)

  override def findById(id: Option[String]): OptionT[F, Option[LimsSpecimen]] = {
    val fa = findByIdSql(id)
      .option
      .transact(xa)

    OptionT.liftF(fa)
  }

  override def getMaxDateProcessed: OptionT[F, Option[Instant]] =
    OptionT(
      getMaxDateProcessedSql.option.transact(xa)
        .handleErrorWith{
          e =>
            for {
              _ <- logs.log(Chain.one(WrapThrowable(e).printStackTraceAsString))
            } yield None
        }
    )
}

object DoobieLimsSpecimenRepositoryInterpreter {
  def apply[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F])(implicit logs: MonadLog[F, Chain[String]]): DoobieLimsSpecimenRepositoryInterpreter[F] =
    new DoobieLimsSpecimenRepositoryInterpreter(xa)
}
