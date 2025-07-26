package typingTest.dictionary.repository

import typingTest.dictionary.model.Dictionary
/*
 * This repository caches dictionaries in memory for faster access.
 * It wraps an existing DictionaryRepository and caches the results of getAllDictionaries
 *  and getDictionaryByName.
 *  Be aware that if the underlying repository values change, this cache will not reflect those changes.
 *  to refresh the cache, you need to create a new instance of CachedRepository.
 */
class CachedRepository(repo: DictionaryRepository) extends DictionaryRepository:
  private val cachedDictionaries: Map[String, Dictionary] =
    repo.getAllDictionaries
      .map(a => a.name -> a)
      .toMap

  override def getAllDictionaries: Seq[Dictionary] =
    cachedDictionaries.values.toSeq
  override def getDictionaryByName(name: String): Option[Dictionary] =
    cachedDictionaries.get(name)
  override def close(): Unit = repo.close()
