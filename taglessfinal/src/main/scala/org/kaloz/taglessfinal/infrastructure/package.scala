package org.kaloz.taglessfinal

package object infrastructure {

  trait ApiResponse

  case class ErrorResponse(errorCode: String)

}
