package com.eztier.redcap.entity.randmock
package config

final case class AppConfig
(
  http: HttpInstanceConfig,
  db: DatabaseInstanceConfig
)