package com.eztier.redcap.entity.limsmock
package config

final case class AppConfig
(
  http: HttpInstanceConfig,
  db: DatabaseInstanceConfig
)