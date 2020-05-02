/*
// https://stackoverflow.com/questions/55049985/how-to-convert-generic-potentially-nested-map-mapstring-any-to-case-class-usi/55355685#55355685
*/
package com.eztier
package common

import shapeless.Lazy
import java.time.Instant

object CaseClassFromMap {
  import io.circe._
  import io.circe.syntax._

  def mapToJson(map: Map[String, Any]): Json =
    map.mapValues(anyToJson).asJson

  def anyToJson(any: Any): Json = any match {
    case n: Int => n.asJson
    case n: Long => n.asJson
    case n: Double => n.asJson
    case s: String => s.asJson
    case i: Instant => i.asJson
    case true => true.asJson
    case false => false.asJson
    case null | None => None.asJson
    case list: List[_] => list.map(anyToJson).asJson
    case list: Vector[_] => list.map(anyToJson).asJson
    case Some(any) => anyToJson(any)
    case map: Map[_, _] => mapToJson(map.asInstanceOf[Map[String, Any]])
  }

  def mapToCaseClass[T](map: Map[String, Any])(implicit decoder: Lazy[Decoder[T]]): Either[io.circe.Error, T] = {
    implicit val d: Decoder[T] = decoder.value
    mapToJson(map).as[T]
  }


}
