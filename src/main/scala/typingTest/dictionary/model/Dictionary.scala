package typingTest.dictionary.model

/** Represents metadata for a dictionary used in typing tests
  *
  * @param name
  *   The name of the dictionary (e.g., "italian-10k")
  * @param filePath
  *   path to the file containing the words
  */

case class Dictionary(
    name: String,
    filePath: String
)

case class DictionaryJson(
    name: String,
    words: Seq[String]
)

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
object DictionaryJson:
  implicit val codec: JsonValueCodec[DictionaryJson] = JsonCodecMaker.make
