package com.eztier.labvantage.entity.limsmock
package infrastructure.doobie.interpreters

import cats.implicits._
import cats.Applicative
import cats.data.{Chain, OptionT}
import cats.effect.{Bracket, Sync}
import doobie.{ConnectionIO, Query0, Transactor, _}
import doobie.implicits._
import fs2.Stream
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

private object LimsSpecimenRemoteSQL {
  /* We require conversion for date time */
  implicit val DateTimeMeta: Meta[Instant] =
    Meta[java.sql.Timestamp].imap(_.toInstant)(java.sql.Timestamp.from)

  val listFragment = fr"""
    select
    null id,
    s_samplefamily.sstudyid SSTUDYID,
    s_study.u_redcap_projectid REDCAPID,
    s_subject.u_mrn U_MRN,
    s_subject.u_firstname U_FIRSTNAME,
    s_subject.u_lastname U_LASTNAME,
    to_char(s_subject.birthdt, 'YYYY-MM-DD') as BIRTHDATE,
    (case when s_subject.u_deidentifiedsubject is null then s_participant.u_studylinkedid else s_subject.u_deidentifiedsubject end) STUDYLINKID,
    (case when s_study.u_isanonymous is null then 0 else 1 end) USE_STUDYLINKID,
    s_sample.s_sampleid as SAMPLEKEY,
    ('LV ParticipantID:'||s_samplefamily.participantid||';EVENT:'||(select eventlabel from s_participantevent where s_participanteventid = s_samplefamily.participanteventid)||';SPR:'||s_samplefamily.u_nyusurgicalpathid||';SAMPLE_PARENTID:'||LV_SMSQUERY.GETPARENTLIST(s_sample.s_sampleid)||';TISSUE_STATUS:'||s_sample.u_tissuestatus||';QUANTITY:'||(select trackitem.qtycurrent from trackitem where trackitem.linkkeyid1 = s_sample.s_sampleid)||';UNIT:'||(select trackitem.qtyunits from trackitem where trackitem.linkkeyid1 = s_sample.s_sampleid)||';CONTAINER_TYPE:'||(select trackitem.containertypeid from trackitem where trackitem.linkkeyid1 = s_sample.s_sampleid)||';STORAGE_STATUS:'||s_sample.storagestatus||';LOCATION:'||(select min(labelpath) from storageunit,trackitem where trackitem.linksdcid ='Sample' and trackitem.linkkeyid1 = s_sample.s_sampleid and currentstorageunitid is not null and currentstorageunitid = storageunitid)) SAMPLEVALUE,
    to_char(s_samplefamily.collectiondt,'YYYY-MM-DD') as SAMPLE_COLLECTION_DATE,
    to_char(s_sample.createdt,'YYYY-MM-DD HH24:MI:SS') CREATEDATE,
    to_char(s_sample.moddt,'YYYY-MM-DD HH24:MI:SS') MODIFYDATE,
    0 processed,
    null date_processed,
    null response,
    null error
    from s_sample inner join s_samplefamily on s_sample.samplefamilyid = s_samplefamily.s_samplefamilyid inner join s_sampletype on s_sampletype.s_sampletypeid = s_sample.sampletypeid inner join s_participant on s_samplefamily.participantid = s_participant.s_participantid inner join s_study on s_participant.sstudyid = s_study.s_studyid
    inner join s_subject on s_subject.s_subjectid = s_participant.subjectid
    where s_study.u_redcapintegrationflag = 'Y' and (LV_SMSQUERY.GETPARENTLIST(s_sample.s_sampleid) is null) and s_samplefamily.collectiondt is not null
  """

  val listCriteriaFragment: Option[Instant] => Fragment =
    maybeDate => maybeDate match {
      case Some(a) => fr" and s_sample.moddt > ${a}"
      case None => Fragment.empty
    }

  def listSql(lastModifyDate: Option[Instant] = None): Query0[LimsSpecimen] =
    (listFragment ++ listCriteriaFragment(lastModifyDate) ++ fr" order by s_sample.moddt").query
}

class DoobieLimsSpecimenRemoteRepositoryInterpreter[F[_]: Bracket[?[_], Throwable]](val xa: Transactor[F])(implicit logs: MonadLog[F, Chain[String]])
  extends LimsSpecimenRemoteAlgebra[F] {
  import LimsSpecimenRemoteSQL._

  override def list(lastModifyDate: Option[Instant] = None): Stream[F, LimsSpecimen] = 
    listSql(lastModifyDate).stream.transact(xa)
}

object DoobieLimsSpecimenRemoteRepositoryInterpreter {
  def apply[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F])(implicit logs: MonadLog[F, Chain[String]]): DoobieLimsSpecimenRemoteRepositoryInterpreter[F] =
    new DoobieLimsSpecimenRemoteRepositoryInterpreter(xa)
}
