package com.eztier.redcap.client
package domain

import java.time.Instant

case class ProjectToken
(
  id: Option[Long] = None,
  project_id: Option[String] = None,
  token: Option[String] = None,
  ts: Option[Instant] = None
)
