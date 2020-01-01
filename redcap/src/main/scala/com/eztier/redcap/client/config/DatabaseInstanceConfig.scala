package com.eztier.redcap.client
package config

case class DatabaseInstanceConfig
(
  local: DatabaseConfig,
  dwh: DatabaseConfig
)
