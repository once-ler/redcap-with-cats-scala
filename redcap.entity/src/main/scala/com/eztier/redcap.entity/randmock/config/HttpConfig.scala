package com.eztier.redcap.entity.randmock
package config

case class HttpConfig
(
  url: String,
  token: Option[String],
  odm: Option[String],
  form: Option[String]
)
