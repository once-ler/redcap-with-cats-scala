package com.eztier.redcap.client
package config

final case class AppConfig
(
  http: HttpInstanceConfig,
  db: DatabaseInstanceConfig
)