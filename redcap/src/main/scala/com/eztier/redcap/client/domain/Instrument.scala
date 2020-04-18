package com.eztier.redcap.client
package domain

import io.circe.generic.extras._

// @ConfiguredJsonCodec
case class Instrument
(
  InstrumentName: Option[String] = None,
  InstrumentLabel: Option[String] = None
)
