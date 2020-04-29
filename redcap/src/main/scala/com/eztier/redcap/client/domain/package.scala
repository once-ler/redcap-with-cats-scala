package com.eztier
package redcap.client

import java.time.format.DateTimeFormatter

import io.circe.{Decoder, Encoder}
import java.time.{Instant, LocalDate}

import scala.util.Try
import common.Util._

package object domain {
  type Metadata = List[Field]
}
