package typingTest.dictionary.loader

import typingTest.dictionary.model.{Dictionary, DictionaryJson}

import scala.collection.concurrent.TrieMap
import scala.io.Source
import scala.util.{Failure, Success, Try}
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import java.nio.file.{Files, Paths}

/** Trait for loading dictionary words from files
  */
trait DictionaryLoader:
  type Words = Seq[String]

  /** Loads words from a dictionary file
    *
    * @param dictionary
    *   The dictionary to load words from
    * @return
    *   A Seq of words from the dictionary
    */
  def loadWords(dictionary: Dictionary): Words

/** Implementation of DictionaryLoader that loads words from files with caching
  */
class FileDictionaryLoader extends DictionaryLoader:
  private val cache = TrieMap[String, Words]()

  override def loadWords(dictionary: Dictionary): Words =
    cache.getOrElseUpdate(dictionary.name, loadFromFile(dictionary))

  private def loadFromFile(dictionary: Dictionary): Words =
    Try {
      val source = Source.fromFile(dictionary.filePath)
      try
        source.getLines().toVector
      finally
        source.close()
    } match
      case Success(words) => words
      case Failure(error) =>
        println(
          s"Error loading dictionary ${dictionary.name}: ${error.getMessage}"
        )
        Vector.empty

/** Implementation of DictionaryLoader that loads words from the 'words' field
  * of a JSON file, otherwise returns Seq.empty. Use jsoniter-scala for parsing.
  */
class JsonDictionaryLoader extends DictionaryLoader:
  private val cache = TrieMap[String, Words]()

  override def loadWords(dictionary: Dictionary): Words =
    cache.getOrElseUpdate(dictionary.name, loadFromFile(dictionary))

  private def loadFromFile(dictionary: Dictionary): Words =
    try
      val bytes = Files.readAllBytes(Paths.get(dictionary.filePath))
      val dict = readFromArray[DictionaryJson](bytes)
      dict.words
    catch case _: Throwable => Seq.empty

class MixedDictionaryLoader(extensionLoaders: Seq[(String, DictionaryLoader)])
    extends DictionaryLoader:
  private val loaderMap: Map[String, DictionaryLoader] = extensionLoaders.toMap

  override def loadWords(dictionary: Dictionary): Words =
    val ext =
      dictionary.filePath.split('.').lastOption.map("." + _).getOrElse("")
    loaderMap.get(ext) match
      case Some(loader) => loader.loadWords(dictionary)
      case None         => Seq.empty
