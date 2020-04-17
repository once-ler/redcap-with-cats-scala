package com.eztier
package common

import fs2.{Pipe, Stream}
import java.io.{PrintWriter, StringWriter}
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}
import shapeless._
import shapeless.ops.record._
import shapeless.ops.hlist.ToTraversable

object Util {
  def filterLeft[F[_], A, B]: Pipe[F, Either[A, B], B] = _.flatMap {
    case Right(r) => Stream.emit(r)
    case Left(_) => Stream.empty
  }

  def filterRight[F[_], A, B]: Pipe[F, Either[A, B], A] = _.flatMap {
    case Left(e) => Stream.emit(e)
    case Right(_) => Stream.empty
  }

  def filterSome[F[_], A]: Pipe[F, Option[A], A] = _.flatMap {
    case Some(e) => Stream.emit(e)
    case None => Stream.empty
  }

  // Author: https://svejcar.dev/posts/2019/10/22/extracting-case-class-field-names-with-shapeless/
  // Also: https://gist.github.com/lunaryorn/4b7becbea955ae909af7426d2e2e166c
  trait Attributes[T] {
    def fieldNames: List[String]
  }

  object Attributes {
    implicit def toAttributes[T, Repr <: HList, KeysRepr <: HList](
      implicit gen: LabelledGeneric.Aux[T, Repr],
      keys: Keys.Aux[Repr, KeysRepr],
      traversable: ToTraversable.Aux[KeysRepr, List, Symbol]
    ): Attributes[T] = new Attributes[T] {
      override def fieldNames: List[String] = keys().toList.map(_.name)
    }

    def apply[T](implicit attributes: Lazy[Attributes[T]]): Attributes[T] = attributes.value
  }

  def getCCFieldNames[A](implicit attributes: Lazy[Attributes[A]]): List[String] =
    attributes.value.fieldNames

  def delimitedStringToMap[A](str: Option[String], delim: Char = '^')(implicit attributes: Lazy[Attributes[A]]): Map[String, String] = {
    val h = getCCFieldNames[A]

    val e = str.fold(List[String]())(_.split(delim).toList)
      .padTo(h.length, "")

    (h zip e).toMap
  }

  // Empty string becomes Some(), we want None.
  implicit class OptionEmptyStringToNone(fa: Option[String]) {
    def toNoneIfEmpty = fa.flatMap(a => if (a.length == 0) None else Some(a))
  }

  def csvToCC[A](converter: CSVConverter[List[A]], str: Option[String], default: A) = {
    converter.from(str.fold("")(a => a))
      .fold(e => List[A](), s => s)
      .headOption.fold(default)(a => a)
  }

  private val defaultDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  def stringToInstant(dateTimeString: String, dateTimePattern: Option[String] = None) = {
    val dateTimeFormatter = dateTimePattern.fold(defaultDateTimeFormatter)(a => DateTimeFormatter.ofPattern(a))

    val maybeLocalDateTime = scala.util.Try(LocalDateTime.parse(dateTimeString, dateTimeFormatter)).fold(e => LocalDateTime.now(), a => a)

    val zoneOffset = OffsetDateTime.now().getOffset
    maybeLocalDateTime.toInstant(zoneOffset)
  }

  def instantToString(instant: Instant, dateTimePattern: Option[String] = None): String = {
    val dateTimeFormatter = dateTimePattern.fold(defaultDateTimeFormatter)(a => DateTimeFormatter.ofPattern(a))

    LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(dateTimeFormatter)
  }

  def instantToXMLGregorianCalendar(instant: Instant): XMLGregorianCalendar = {
    val instantToLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate
    DatatypeFactory.newInstance().newXMLGregorianCalendar(instantToLocalDate.toString)
  }

  // Stack trace to string.
  implicit class WrapThrowable[E <: Throwable](e: E) {
    def printStackTraceAsString: String = {
      val sw = new StringWriter
      e.printStackTrace(new PrintWriter(sw))
      e.getMessage match {
        case a if a != null => a.concat(sw.toString)
        case _ => sw.toString
      }
    }
  }
}
