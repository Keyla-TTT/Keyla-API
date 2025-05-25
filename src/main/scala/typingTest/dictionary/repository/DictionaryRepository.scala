package typingTest.dictionary.repository

import typingTest.dictionary.model.Dictionary

/**
 * Repository interface for dictionaries
 */
trait DictionaryRepository:
  /**
   * Gets all available dictionaries
   *
   * @return A sequence of all dictionaries
   */
  def getAllDictionaries(): Seq[Dictionary]
  
  /**
   * Gets dictionaries by language
   *
   * @param language The language to filter by
   * @return A sequence of dictionaries in the specified language
   */
  def getDictionariesByLanguage(language: String): Seq[Dictionary]
  
  /**
   * Gets a specific dictionary by name
   *
   * @param name The name of the dictionary
   * @return An Option containing the dictionary if found, None otherwise
   */
  def getDictionaryByName(name: String): Option[Dictionary]
