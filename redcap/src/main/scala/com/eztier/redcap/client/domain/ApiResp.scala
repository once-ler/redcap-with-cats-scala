package com.eztier.redcap.client
package domain

sealed trait ApiResp
case class ApiError(body: Json) extends ApiResp
case class ApiOk(body: Json) extends ApiResp
