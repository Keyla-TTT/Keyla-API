package users_management.service

import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.MongoDBContainer
import users_management.factory.UserFactory
import users_management.repository.{DatabaseInfos, MongoProfileRepository}
import scala.compiletime.uninitialized

class TestMemoryServiceWithMongoDockerRepository
    extends AnyFunSuite
    with BeforeAndAfter
    with BeforeAndAfterAll
    with Matchers:
  private val mongoContainer = new MongoDBContainer("mongo:6.0")
  private var service: ProfileService = uninitialized
  private var repository: MongoProfileRepository = uninitialized
  private val factory = new UserFactory()

  override def beforeAll(): Unit =
    mongoContainer.start()

  override def afterAll(): Unit =
    mongoContainer.stop()

  before {
    val mongoUri =
      s"mongodb://${mongoContainer.getHost}:${mongoContainer.getFirstMappedPort}"
    val dbInfos = DatabaseInfos("tests", mongoUri, "profiles")
    repository = new MongoProfileRepository(dbInfos)
    service = ProfileServiceImpl(repository)
    repository.deleteAll()
  }

  after {
    if repository != null then
      repository.deleteAll()
      repository.close()
  }

  test("createProfileShouldGenerateId") {
    val profile = factory.createUserProfile(
      "Mario",
      "mario@test.com",
      Set("user")
    )
    val created = service.createProfile(profile)
    created.id shouldBe defined
    created.name shouldBe "Mario"
  }

  test("getProfileShouldReturnExistingProfile") {
    val profile = factory.createUserProfile(
      "Luigi",
      "luigi@test.com",
      Set("admin")
    )
    val created = service.createProfile(profile)
    service.getProfile(created.id.get) shouldBe Some(created)
  }

  test("getNonExistingProfileShouldReturnNone") {
    service.getProfile("507f1f77bcf86cd799439011") shouldBe None
  }

  test("updateExistingProfileShouldSucceed") {
    val profile = factory.createUserProfile(
      "Mario",
      "mario@test.com",
      Set("user")
    )
    val created = service.createProfile(profile)
    val updated = factory
      .createUserProfile(
        "Super Mario",
        created.email,
        Set("admin")
      )
      .copy(id = created.id)
    service.updateProfile(updated) shouldBe Some(updated)
  }

  test("updateNonExistingProfileShouldReturnNone") {
    val profile = factory.createUserProfile(
      "Mario",
      "mario@test.com",
      Set("user")
    )
    val nonExisting = profile.copy(id = Some("507f1f77bcf86cd799439011"))
    service.updateProfile(nonExisting) shouldBe None
  }

  test("deleteExistingProfileShouldReturnTrue") {
    val profile = factory.createUserProfile(
      "Mario",
      "mario@test.com",
      Set("user")
    )
    val created = service.createProfile(profile)
    service.deleteProfile(created.id.get) shouldBe true
    service.getProfile(created.id.get) shouldBe None
  }

  test("deleteNonExistingProfileShouldReturnFalse") {
    service.deleteProfile("507f1f77bcf86cd799439011") shouldBe false
  }

  test("listProfilesShouldReturnAllProfiles") {
    val profile1 = factory.createUserProfile(
      "Mario",
      "mario@test.com",
      Set("user")
    )
    val profile2 = factory.createUserProfile(
      "Luigi",
      "luigi@test.com",
      Set("admin")
    )
    val created1 = service.createProfile(profile1)
    val created2 = service.createProfile(profile2)
    val profiles = service.listProfiles()
    profiles should contain allOf (created1, created2)
    profiles should have size 2
  }

  test("listProfilesOnEmptyDatabaseShouldReturnEmptyList") {
    service.listProfiles() shouldBe empty
  }

  test("deleteAllProfilesShouldReturnTrueWhenProfilesExist") {
    val profile = factory.createUserProfile(
      "Mario",
      "mario@test.com",
      Set("user")
    )
    service.createProfile(profile)
    service.deleteAllProfiles() shouldBe true
    service.listProfiles() shouldBe empty
  }

  test("deleteAllProfilesShouldReturnFalseWhenNoProfiles") {
    service.listProfiles() shouldBe empty
    service.deleteAllProfiles() shouldBe false
  }
