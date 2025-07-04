package users_management.service

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import users_management.factory.UserFactory
import users_management.model.UserProfile
import users_management.repository.InMemoryProfileRepository

class TestMemoryServiceWithInMemoryRepository extends AnyFlatSpec with Matchers:

  def createCleanTestEnvironment()
      : (ProfileService, InMemoryProfileRepository, UserFactory) =
    val repository = new InMemoryProfileRepository()
    val factory = new UserFactory()
    val service = ProfileServiceImpl(repository)
    (service, repository, factory)

  "MemoryProfileService" should "create a new profile with generated id" in {
    val (service, _, factory) = createCleanTestEnvironment()
    val profile = factory.createUserProfile(
      "Mario",
      "mario@test.com",
      Set("user")
    )

    val created = service.createProfile(profile)
    created.id shouldBe defined
    created.name shouldBe "Mario"
    created.email shouldBe "mario@test.com"
    created.settings shouldBe Set("user")
  }

  it should "retrieve an existing profile by id" in {
    val (service, _, factory) = createCleanTestEnvironment()
    val profile = factory.createUserProfile(
      "Luigi",
      "luigi@test.com",
      Set("user")
    )
    val created = service.createProfile(profile)

    val retrieved = service.getProfile(created.id.get)
    retrieved shouldBe Some(created)
  }

  it should "return None when getting non-existing profile" in {
    val (service, _, factory) = createCleanTestEnvironment()
    service.getProfile("non-existing") shouldBe None
  }

  it should "update an existing profile" in {
    val (service, _, factory) = createCleanTestEnvironment()
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

  it should "return None when updating non-existing profile" in {
    val (service, _, factory) = createCleanTestEnvironment()
    val profile = factory.createUserProfile(
      "Mario",
      "mario@test.com",
      Set("user")
    )
    val nonExisting = profile.copy(id = Some("non-existing"))
    service.updateProfile(nonExisting) shouldBe None
  }

  it should "delete an existing profile" in {
    val (service, _, factory) = createCleanTestEnvironment()
    val profile = factory.createUserProfile(
      "Mario",
      "mario@test.com",
      Set("user")
    )
    val created = service.createProfile(profile)
    service.deleteProfile(created.id.get) shouldBe true
    service.getProfile(created.id.get) shouldBe None
  }

  it should "return false when deleting non-existing profile" in {
    val (service, _, factory) = createCleanTestEnvironment()
    service.deleteProfile("non-existing") shouldBe false
  }

  it should "delete all profiles" in {
    val (service, _, factory) = createCleanTestEnvironment()
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
    service.createProfile(profile1)
    service.createProfile(profile2)
    service.deleteAllProfiles() shouldBe true
  }

  it should "return false when deleting all profiles if none exist" in {
    val (service, _, _) = createCleanTestEnvironment()
    service.deleteAllProfiles() shouldBe false
  }

  it should "list all profiles" in {
    val (service, _, factory) = createCleanTestEnvironment()
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

  it should "return empty list when no profiles exist" in {
    val (service, _, factory) = createCleanTestEnvironment()
    service.listProfiles() shouldBe empty
  }
