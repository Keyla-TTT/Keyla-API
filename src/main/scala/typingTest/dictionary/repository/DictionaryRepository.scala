package typingTest.dictionary.repository

import common.Repository
import typingTest.dictionary.model.Dictionary

/** Repository interface for dictionaries
  */
trait DictionaryRepository extends Repository:
  /** Gets all available dictionaries
    *
    * @return
    *   A sequence of all dictionaries
    */
  def getAllDictionaries: Seq[Dictionary]

  /** Gets a specific dictionary by name
    *
    * @param name
    *   The name of the dictionary
    * @return
    *   An Option containing the dictionary if found, None otherwise
    */
  def getDictionaryByName(name: String): Option[Dictionary]
