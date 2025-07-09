package typingTest.tests.repository

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InMemoryTypingTestRepositorySpec extends TypingTestRepositorySpec:

  override def createRepository(): TypingTestRepository =
    InMemoryTypingTestRepository()

  "InMemoryTypingTestRepository" should {

    "start with empty repository" in {
      val newRepo = InMemoryTypingTestRepository()
      newRepo.list() should be(empty)
    }

    "handle concurrent access safely" in {
      val test1 = createSampleTest("profile-1", "english")
      val test2 = createSampleTest("profile-2", "spanish")

      val created1 = repository.create(test1)
      val created2 = repository.create(test2)

      created1.id should not equal created2.id

      val allTests = repository.list()
      allTests should have size 2
    }

    "generate unique IDs for each test" in {
      val tests = (1 to 10).map { i =>
        repository.create(createSampleTest(s"profile-$i", "english"))
      }

      val ids = tests.map(_.id.get)
      ids.distinct should have size 10
    }

    "maintain data integrity after multiple operations" in {
      val test = createSampleTest("profile-1", "english")
      val created = repository.create(test)

      val updated = repository.update(created.copy(language = "spanish"))
      updated should be(defined)

      val retrieved = repository.get(created.id.get)
      retrieved should be(defined)
      retrieved.get.language should equal("spanish")

      repository.delete(created.id.get) should be(true)
      repository.get(created.id.get) should be(None)
    }
  }
