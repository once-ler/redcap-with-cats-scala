package com.eztier.redcap.client
package config

case class HttpConfig
(
  url: String,
  token: Option[String],
  odm: Option[String]
)
