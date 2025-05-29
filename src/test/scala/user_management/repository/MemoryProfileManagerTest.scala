package user_management.repository

import org.scalatest.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import users_management.repository.MemoryProfileManager

import java.util.UUID

class MemoryProfileManagerTest extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  var profileManager: MemoryProfileManager = _
  val testProfileName = "Test User"
  val testProfileEmail = "test@example.com"
  val testProfilePassword = "secure123"
  val testProfileSettings: Set[String] = Set("theme:dark", "lang:en")

  override def beforeEach(): Unit = {
    profileManager = MemoryProfileManager()
  }

  "InMemoryProfileManager" should {

    "create a new profile with unique ID" in {
      val profile = profileManager.createProfile(testProfileName, testProfileEmail, testProfilePassword, testProfileSettings)

      profile.id should not be null
      profile.name shouldBe testProfileName
      profile.email shouldBe testProfileEmail
      profile.password shouldBe testProfilePassword
      profile.settings shouldBe testProfileSettings
    }

    "retrieve a created profile by ID" in {
      val createdProfile = profileManager.createProfile(testProfileName, testProfileEmail, testProfilePassword, testProfileSettings)
      val retrievedProfile = profileManager.getProfile(createdProfile.id)

      retrievedProfile shouldBe Some(createdProfile)
    }

    "return None when getting a non-existent profile" in {
      val nonExistentId = UUID.randomUUID()
      profileManager.getProfile(nonExistentId) shouldBe None
    }

    "update an existing profile" in {
      val createdProfile = profileManager.createProfile(testProfileName, testProfileEmail, testProfilePassword, testProfileSettings)
      val newName = "Updated Name"
      val newSettings = Set("theme:light", "lang:it")

      val updatedProfile = profileManager.updateProfile(
        createdProfile.id,
        Some(newName),
        None,
        None,
        Some(newSettings)
      )

      updatedProfile shouldBe defined
      updatedProfile.get.name shouldBe newName
      updatedProfile.get.email shouldBe testProfileEmail // unchanged
      updatedProfile.get.password shouldBe testProfilePassword // unchanged
      updatedProfile.get.settings shouldBe newSettings
    }

    "partially update a profile" in {
      val createdProfile = profileManager.createProfile(testProfileName, testProfileEmail, testProfilePassword, testProfileSettings)
      val newEmail = "updated@example.com"

      val updatedProfile = profileManager.updateProfile(
        createdProfile.id,
        None,
        Some(newEmail),
        None,
        None
      )

      updatedProfile shouldBe defined
      updatedProfile.get.name shouldBe testProfileName // unchanged
      updatedProfile.get.email shouldBe newEmail
      updatedProfile.get.password shouldBe testProfilePassword // unchanged
      updatedProfile.get.settings shouldBe testProfileSettings // unchanged
    }

    "return None when updating a non-existent profile" in {
      val nonExistentId = UUID.randomUUID()
      val result = profileManager.updateProfile(
        nonExistentId,
        Some("New Name"),
        None,
        None,
        None
      )

      result shouldBe None
    }

    "delete an existing profile" in {
      val createdProfile = profileManager.createProfile(testProfileName, testProfileEmail, testProfilePassword, testProfileSettings)
      val deleteResult = profileManager.deleteProfile(createdProfile.id)

      deleteResult shouldBe true
      profileManager.getProfile(createdProfile.id) shouldBe None
    }

    "return false when deleting a non-existent profile" in {
      val nonExistentId = UUID.randomUUID()
      profileManager.deleteProfile(nonExistentId) shouldBe false
    }

    "list all created profiles" in {
      val profile1 = profileManager.createProfile("User 1", "user1@test.com", "pass1", Set("setting1"))
      val profile2 = profileManager.createProfile("User 2", "user2@test.com", "pass2", Set("setting2"))

      val allProfiles = profileManager.listProfiles()

      allProfiles should contain allOf(profile1, profile2)
      allProfiles.size shouldBe 2
    }

    "return empty list when no profiles exist" in {
      profileManager.listProfiles() shouldBe empty
    }

  }
}