package users_management.repository

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.MongoDBContainer
import users_management.model.UserProfile

import scala.compiletime.uninitialized

class TestMongoDockerRepo
    extends AnyFunSuite
    with BeforeAndAfter
    with BeforeAndAfterAll
    with Matchers:
  private val mongoContainer = new MongoDBContainer("mongo:6.0")

  private var repository: MongoProfileRepository = uninitialized
  private val testProfile = UserProfile(
    None,
    "Test User",
    "test@example.com",
    Set("setting1", "setting2")
  )

  override def beforeAll(): Unit =
    mongoContainer.start()

  override def afterAll(): Unit =
    mongoContainer.stop()

  before:
    val dbInfos = DatabaseInfos(
      collectionName = "profiles",
      mongoUri =
        s"mongodb://${mongoContainer.getHost}:${mongoContainer.getFirstMappedPort}",
      databaseName = "profiles_db"
    )
    repository = new MongoProfileRepository(dbInfos)
    repository.deleteAll()

  after:
    if repository != null then repository.close()

  test("createdProfileHasGeneratedId") {
    val created = repository.create(testProfile)
    assert(created.id.isDefined)
    assert(created.name == testProfile.name)
  }

  test("getExistingProfileReturnsCorrectProfile") {
    val created = repository.create(testProfile)
    val retrieved = repository.get(created.id.get)
    assert(retrieved.isDefined)
    assert(retrieved.get == created)
  }

  test("getNonExistingProfileReturnsNone") {
    val result = repository.get("507f1f77bcf86cd799439011")
    assert(result.isEmpty)
  }

  test("updateExistingProfileSucceeds") {
    val created = repository.create(testProfile)
    val updatedProfile = created.copy(name = "Updated Name")
    val result = repository.update(updatedProfile)
    assert(result.isDefined)
    assert(result.get.name == "Updated Name")
  }

  test("updateNonExistingProfileReturnsNone") {
    val nonExistingProfile =
      testProfile.copy(id = Some("507f1f77bcf86cd799439011"))
    val result = repository.update(nonExistingProfile)
    assert(result.isEmpty)
  }

  test("deleteExistingProfileReturnsTrue") {
    val created = repository.create(testProfile)
    assert(repository.delete(created.id.get))
    assert(repository.get(created.id.get).isEmpty)
  }

  test("deleteNonExistingProfileReturnsFalse") {
    assert(!repository.delete("507f1f77bcf86cd799439011"))
  }

  test("listReturnsAllProfiles") {
    val profile1 = repository.create(testProfile)
    val profile2 =
      repository.create(testProfile.copy(email = "test2@example.com"))
    val profiles = repository.list()
    assert(profiles.size == 2)
    assert(profiles.contains(profile1))
    assert(profiles.contains(profile2))
  }

  test("listReturnsEmptyListForEmptyDatabase") {
    val profiles = repository.list()
    assert(profiles.isEmpty)
  }

  test("invalidIdFormatReturnsNone") {
    val result = repository.get("invalid-id")
    assert(result.isEmpty)
  }
