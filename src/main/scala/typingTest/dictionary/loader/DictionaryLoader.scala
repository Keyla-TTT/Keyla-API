package typingTest.dictionary.loader

import typingTest.dictionary.model.Dictionary
import scala.io.Source
import scala.util.{Try, Success, Failure}
import scala.collection.concurrent.TrieMap

/**
 * Trait for loading dictionary words from files
 */
trait DictionaryLoader:
  type Words = Seq[String]
  /**
   * Loads words from a dictionary file
   *
   * @param dictionary The dictionary to load words from
   * @return A Seq of words from the dictionary
   */
  def loadWords(dictionary: Dictionary): Words

/**
 * Implementation of DictionaryLoader that loads words from files with caching
 */
class FileDictionaryLoader extends DictionaryLoader:
  private val cache = TrieMap[String, Words]()
  
  override def loadWords(dictionary: Dictionary): Words =
    cache.getOrElseUpdate(dictionary.filePath, loadFromFile(dictionary))
    
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
        println(s"Error loading dictionary ${dictionary.name}: ${error.getMessage}")
        Vector.empty 