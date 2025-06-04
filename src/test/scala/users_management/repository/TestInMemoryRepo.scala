package users_management.repository

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfter
import users_management.model.UserProfile

class TestInMemoryRepo extends AnyFunSuite with Matchers with BeforeAndAfter:

  private var repo: InMemoryProfileRepository = _
  private val testProfile = UserProfile(None, "Mario", "mario@email.com", "pass", Set("a"))

  before {
    repo = new InMemoryProfileRepository()
  }

  test("create generates a new ID and returns the created profile") {
    val created = repo.create(testProfile)
    created.id shouldBe defined
    created.name shouldBe testProfile.name
    created.email shouldBe testProfile.email
    repo.get(created.id.get) shouldBe Some(created)
  }

  test("get returns a profile by ID") {
    repo.get("non-esistente") shouldBe None
  }

  test("update modifies an existing profile") {
    val created = repo.create(testProfile)
    val updated = created.copy(name = "Mario Updated")
    repo.update(updated) shouldBe Some(updated)
    repo.get(created.id.get).get.name shouldBe "Mario Updated"
  }

  test("update returns None if the profile does not exist") {
    val nonExisting = UserProfile(Some("non-esistente"), "Mario", "mario@email.com", "pass", Set("a"))
    repo.update(nonExisting) shouldBe None
  }

  test("delete removes an existing profile") {
    val created = repo.create(testProfile)
    repo.delete(created.id.get) shouldBe true
    repo.get(created.id.get) shouldBe None
  }

  test("delete returns false if the profile does not exist") {
    repo.delete("non-esistente") shouldBe false
  }

  test("deleteAll removes all profiles and returns true") {
    val profile1 = repo.create(testProfile)
    val profile2 = repo.create(testProfile.copy(email = "luigi@email.com"))
    repo.deleteAll() shouldBe true
    repo.list() shouldBe empty
  }

  test("deleteAll returns false if there are no profiles to delete") {
    repo.deleteAll() shouldBe false
  }

  test("list returns all profiles") {
    val profile1 = repo.create(testProfile)
    val profile2 = repo.create(testProfile.copy(email = "luigi@email.com"))
    val profiles = repo.list()
    profiles should contain allOf(profile1, profile2)
    profiles should have size 2
  }

  test("list returns empty when no profiles exist") {
    repo.list() shouldBe empty
  }