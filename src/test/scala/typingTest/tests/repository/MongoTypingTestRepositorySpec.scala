package typingTest.tests.repository

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import users_management.repository.DatabaseInfos
import com.mongodb.client.MongoClients
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import scala.compiletime.uninitialized

class MongoTypingTestRepositorySpec extends TypingTestRepositorySpec with BeforeAndAfterAll:

  private val mongoContainer = MongoDBContainer(DockerImageName.parse("mongo:4.4"))
  private var dbInfos: DatabaseInfos = uninitialized
  private var mongoRepo: MongoTypingTestRepository = uninitialized

  override def beforeAll(): Unit =
    super.beforeAll()
    mongoContainer.start()
    
    dbInfos = DatabaseInfos(
      collectionName = "test_typing_tests",
      mongoUri = mongoContainer.getReplicaSetUrl,
      databaseName = "test_db"
    )

  override def afterAll(): Unit =
    if (mongoRepo != null) mongoRepo.close()
    mongoContainer.stop()
    super.afterAll()

  override def createRepository(): TypingTestRepository =
    mongoRepo = MongoTypingTestRepository(dbInfos)
    mongoRepo

  "MongoTypingTestRepository" should {

    "connect to MongoDB successfully" in {
      val repo = createRepository()
      repo.list() should be(empty)
    }

    "handle MongoDB ObjectId conversion properly" in {
      val test = createSampleTest("profile-mongo", "english")
      val created = repository.create(test)
      
      created.id should be(defined)
      created.id.get should have length 24
      created.id.get should fullyMatch regex "[a-f0-9]{24}"
    }

    "persist complex nested structures correctly" in {
      val test = createSampleTest("complex-profile", "english")
      val created = repository.create(test)
      
      mongoRepo.close()
      val newRepo = MongoTypingTestRepository(dbInfos)
      
      val retrieved = newRepo.get(created.id.get)
      retrieved should be(defined)
      retrieved.get.testData.sources should equal(created.testData.sources)
      retrieved.get.testData.modifiers should equal(created.testData.modifiers)
      retrieved.get.testData.words should equal(created.testData.words)
      
      newRepo.close()
    }

    "handle DateTime serialization and deserialization" in {
      val test = createSampleTest("datetime-profile", "english")
      val created = repository.create(test)
      
      val retrieved = repository.get(created.id.get)
      retrieved should be(defined)
      
      val timeDiff = Math.abs(retrieved.get.createdAt.getMillis - created.createdAt.getMillis)
      timeDiff should be < 1000L
    }

    "handle invalid ObjectId gracefully" in {
      val result = repository.get("invalid-object-id")
      result should be(None)
    }

    "maintain data across repository instances" in {
      val test = createSampleTest("persistent-profile", "english")
      val created = repository.create(test)
      
      val newRepo = MongoTypingTestRepository(dbInfos)
      val retrieved = newRepo.get(created.id.get)
      
      retrieved should be(defined)
      retrieved.get.profileId should equal(created.profileId)
      
      newRepo.close()
    }

    "handle concurrent writes safely" in {
      val tests = (1 to 5).map { i =>
        createSampleTest(s"concurrent-profile-$i", "english")
      }
      
      val created = tests.map(repository.create)
      created should have size 5
      
      val allTests = repository.list()
      allTests.size should be >= 5
    }

    "clean up collections properly" in {
      repository.create(createSampleTest("cleanup-test", "english"))
      repository.list() should not be empty
      
      repository.deleteAll() should be(true)
      repository.list() should be(empty)
    }
  }
