package api.services

import api.models.AppError.*
import api.models.users.*
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import users_management.repository.InMemoryProfileRepository

class ProfileServiceSpec extends AsyncWordSpec with Matchers with AsyncIOSpec:

  private def createTestEnvironment()
      : (ProfileService, InMemoryProfileRepository) =
    val repository = new InMemoryProfileRepository()
    val service = ProfileService(repository)
    (service, repository)

  "ProfileService" should {

    "create profile successfully" in {
      val (service, _) = createTestEnvironment()
      val request = CreateProfileRequest(
        name = "New User",
        email = "newuser@example.com",
        settings = Set("setting1", "setting2")
      )

      val result = service.createProfile(request).value.unsafeRunSync()
      result.isRight should be(true)
      val profile = result.toOption.get

      profile.name should equal("New User")
      profile.email should equal("newuser@example.com")
      profile.settings should equal(Set("setting1", "setting2"))
      profile.id should not be empty
    }

    "list all profiles successfully" in {
      val (service, _) = createTestEnvironment()
      val request1 = CreateProfileRequest(
        name = "User 1",
        email = "user1@example.com",
        settings = Set("setting1")
      )
      val request2 = CreateProfileRequest(
        name = "User 2",
        email = "user2@example.com",
        settings = Set("setting2")
      )

      service.createProfile(request1).value.unsafeRunSync().isRight should be(
        true
      )
      service.createProfile(request2).value.unsafeRunSync().isRight should be(
        true
      )

      val result = service.getAllProfiles().value.unsafeRunSync()
      result.isRight should be(true)
      val profileList = result.toOption.get

      profileList.profiles should have size 2
      profileList.profiles.map(_.name) should contain allElementsOf List(
        "User 1",
        "User 2"
      )
    }

    "get profile by id successfully" in {
      val (service, _) = createTestEnvironment()
      val request = CreateProfileRequest(
        name = "Test User",
        email = "test@example.com",
        settings = Set("user")
      )

      val createdProfile =
        service.createProfile(request).value.unsafeRunSync().toOption.get
      val result =
        service.getProfileById(createdProfile.id).value.unsafeRunSync()

      result.isRight should be(true)
      val retrievedProfile = result.toOption.get
      retrievedProfile.id should equal(createdProfile.id)
      retrievedProfile.name should equal("Test User")
      retrievedProfile.email should equal("test@example.com")
    }

    "handle profile not found error" in {
      val (service, _) = createTestEnvironment()
      val result =
        service.getProfileById("non-existent-id").value.unsafeRunSync()

      result.isLeft should be(true)
      result.left.toOption.get shouldBe a[ProfileNotFound]
    }

    "update profile successfully" in {
      val (service, _) = createTestEnvironment()
      val createRequest = CreateProfileRequest(
        name = "Original User",
        email = "original@example.com",
        settings = Set("user")
      )

      val createdProfile =
        service.createProfile(createRequest).value.unsafeRunSync().toOption.get

      val updateRequest = CreateProfileRequest(
        name = "Updated User",
        email = "updated@example.com",
        settings = Set("user", "admin")
      )

      val result = service
        .updateProfile(createdProfile.id, updateRequest)
        .value
        .unsafeRunSync()
      result.isRight should be(true)
      val updatedProfile = result.toOption.get

      updatedProfile.id should equal(createdProfile.id)
      updatedProfile.name should equal("Updated User")
      updatedProfile.email should equal("updated@example.com")
      updatedProfile.settings should equal(Set("user", "admin"))
    }

    "handle update profile not found error" in {
      val (service, _) = createTestEnvironment()
      val updateRequest = CreateProfileRequest(
        name = "Updated User",
        email = "updated@example.com",
        settings = Set("user")
      )

      val result = service
        .updateProfile("non-existent-id", updateRequest)
        .value
        .unsafeRunSync()
      result.isLeft should be(true)
      result.left.toOption.get shouldBe a[ProfileNotFound]
    }

    "delete profile successfully" in {
      val (service, _) = createTestEnvironment()
      val request = CreateProfileRequest(
        name = "User to Delete",
        email = "delete@example.com",
        settings = Set("user")
      )

      val createdProfile =
        service.createProfile(request).value.unsafeRunSync().toOption.get
      val result =
        service.deleteProfile(createdProfile.id).value.unsafeRunSync()

      result.isRight should be(true)
      val deleteResponse = result.toOption.get
      deleteResponse.success should be(true)
      deleteResponse.message should include("deleted successfully")
    }

    "handle delete profile not found error" in {
      val (service, _) = createTestEnvironment()
      val result =
        service.deleteProfile("non-existent-id").value.unsafeRunSync()

      result.isLeft should be(true)
      result.left.toOption.get shouldBe a[ProfileNotFound]
    }
  }
