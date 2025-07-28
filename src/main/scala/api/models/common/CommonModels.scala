package api.models.common

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

case class ErrorResponse(
    message: String,
    code: String,
    statusCode: Int
)

object CommonModels:
  given JsonValueCodec[ErrorResponse] = JsonCodecMaker.make
