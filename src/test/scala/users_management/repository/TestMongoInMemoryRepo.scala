package users_management.repository

import de.flapdoodle.embed.mongo.config.{ImmutableMongodConfig, MongodConfig, Net}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.{MongodExecutable, MongodProcess, MongodStarter}
import de.flapdoodle.embed.process.runtime.Network
import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import users_management.model.UserProfile

class TestMongoInMemoryRepo extends AnyFunSuite with BeforeAndAfter with BeforeAndAfterAll with Matchers:
  private var mongod: MongodExecutable = _
  private var repository: MongoProfileRepository = _
  private val testProfile = UserProfile(None, "Test User", "test@example.com", "password123", Set("setting1", "setting2"))
  private val port = 27017

  override def beforeAll(): Unit =
    val mongodConfig = MongodConfig.builder()
      .version(Version.V3_6_22)
      .net(new Net(port, Network.localhostIsIPv6()))
      .build()

    mongod = MongodStarter.getDefaultInstance.prepare(mongodConfig)
    mongod.start()

  override def afterAll(): Unit = {
    // Ferma MongoDB embedded
    if mongod != null then mongod.stop()
  }

  before {
    val dbInfos = DatabaseInfos(
      collectionName = "profiles",
      mongoUri = s"mongodb://localhost:$port",
      databaseName = "test_db" // Nome arbitrario per i test
    )
    repository = new MongoProfileRepository(dbInfos)
    repository.deleteAll() // Pulisce il database prima di ogni test
  }

  after {
    if repository != null then repository.close()
  }


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
    val nonExistingProfile = testProfile.copy(id = Some("507f1f77bcf86cd799439011"))
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
    val profile2 = repository.create(testProfile.copy(email = "test2@example.com"))
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