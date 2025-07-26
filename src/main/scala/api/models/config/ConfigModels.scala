package api.models.config

import api.services.{
  ConfigEntry,
  ConfigKey,
  ConfigListResponse,
  ConfigUpdateRequest,
  ConfigUpdateResponse,
  SimpleConfigUpdateRequest
}
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import config.*
import sttp.tapir.Schema
import sttp.tapir.generic.auto.*

object ConfigModels:
  given JsonValueCodec[ConfigKey] = JsonCodecMaker.make
  given JsonValueCodec[ConfigEntry] = JsonCodecMaker.make
  given JsonValueCodec[ConfigListResponse] = JsonCodecMaker.make
  given JsonValueCodec[ConfigUpdateRequest] = JsonCodecMaker.make
  given JsonValueCodec[SimpleConfigUpdateRequest] = JsonCodecMaker.make
  given JsonValueCodec[ConfigUpdateResponse] = JsonCodecMaker.make
  given JsonValueCodec[DatabaseConfig] = JsonCodecMaker.make
  given JsonValueCodec[ServerConfig] = JsonCodecMaker.make
  given JsonValueCodec[DictionaryConfig] = JsonCodecMaker.make
  given JsonValueCodec[ThreadPoolConfig] = JsonCodecMaker.make
  given JsonValueCodec[AppConfig] = JsonCodecMaker.make

  given Schema[ConfigKey] = Schema.derived[ConfigKey]
  given Schema[DatabaseConfig] = Schema.derived[DatabaseConfig]
  given Schema[ServerConfig] = Schema.derived[ServerConfig]
  given Schema[DictionaryConfig] = Schema.derived[DictionaryConfig]
  given Schema[ThreadPoolConfig] = Schema.derived[ThreadPoolConfig]
  given Schema[AppConfig] = Schema.derived[AppConfig]
