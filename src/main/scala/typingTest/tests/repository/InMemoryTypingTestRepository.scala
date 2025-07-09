package typingTest.tests.repository

import typingTest.tests.model.PersistedTypingTest

import scala.collection.mutable

class InMemoryTypingTestRepository extends TypingTestRepository:

  private val tests: mutable.Map[String, PersistedTypingTest] = mutable.Map()

  override def get(id: String): Option[PersistedTypingTest] =
    tests.get(id)

  override def create(test: PersistedTypingTest): PersistedTypingTest =
    val id = generateUniqueId()
    val testWithId = test.copy(id = Some(id))
    tests += (id -> testWithId)
    testWithId

  @scala.annotation.tailrec
  private def generateUniqueId(): String =
    val id = java.util.UUID.randomUUID().toString
    id match
      case id if tests.contains(id) => generateUniqueId()
      case _                        => id

  override def update(test: PersistedTypingTest): Option[PersistedTypingTest] =
    test.id.flatMap: id =>
      if tests.contains(id) then
        tests.update(id, test)
        Some(test)
      else None

  override def delete(id: String): Boolean =
    tests.remove(id).isDefined

  override def deleteAll(): Boolean =
    val sizeBefore = tests.size
    tests.clear()
    sizeBefore > 0

  override def list(): List[PersistedTypingTest] =
    tests.values.toList

  override def getByProfileId(profileId: String): List[PersistedTypingTest] =
    tests.values.filter(_.profileId == profileId).toList

  override def getByLanguage(language: String): List[PersistedTypingTest] =
    tests.values.filter(_.language == language).toList

  override def getByProfileIdAndLanguage(
      profileId: String,
      language: String
  ): List[PersistedTypingTest] =
    tests.values
      .filter(test => test.profileId == profileId && test.language == language)
      .toList

  override def getLastNonCompletedByProfileId(
      profileId: String
  ): Option[PersistedTypingTest] =
    tests.values
      .filter(test => test.profileId == profileId && !test.isCompleted)
      .toList
      .sortBy(_.createdAt.getMillis)
      .lastOption

  override def deleteNonCompletedByProfileId(profileId: String): Int =
    val toDelete = tests.values
      .filter(test => test.profileId == profileId && !test.isCompleted)
      .toList
    toDelete.foreach(test => test.id.foreach(tests.remove))
    toDelete.size

  override def getCompletedById(id: String): Option[PersistedTypingTest] =
    tests.get(id).filter(_.isCompleted)
