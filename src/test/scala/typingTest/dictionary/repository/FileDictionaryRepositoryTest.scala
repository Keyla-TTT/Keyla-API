package typingTest.dictionary.repository

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import typingTest.dictionary.model.Dictionary

import java.io.File
import java.nio.file.{Files, Paths}
import scala.compiletime.uninitialized

class FileDictionaryRepositoryTest
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterEach:
  // Test with a temporary directory
  val tempDir: File = Files.createTempDirectory("dict-test").toFile
  val italianDir = new File(tempDir, "italian")
  val englishDir = new File(tempDir, "english")

  // Repository instance to test
  var repo: FileDictionaryRepository = uninitialized

  override def beforeEach(): Unit =
    // Create test directories and files
    italianDir.mkdirs()
    englishDir.mkdirs()
    Files.writeString(new File(italianDir, "1k.txt").toPath, "italian1k")
    Files.writeString(new File(italianDir, "10k.txt").toPath, "italian10k")
    Files.writeString(new File(englishDir, "1k.txt").toPath, "english1k")

    // Create a fresh repository instance
    repo = new FileDictionaryRepository(tempDir.getAbsolutePath)

  override def afterEach(): Unit =
    // Clean up test files
    def deleteRecursively(file: File): Unit =
      if file.isDirectory then
        Option(file.listFiles()).foreach(_.foreach(deleteRecursively))
      file.delete()

    deleteRecursively(tempDir)

    // Recreate the base directory for the next test
    tempDir.mkdirs()

  "FileDictionaryRepository" should "find all dictionaries" in {
    val dictionaries = repo.getAllDictionaries

    dictionaries.size shouldBe 3
    dictionaries.map(_.name).toSet shouldBe Set("1k", "10k", "1k")
    dictionaries.map(_.language).toSet shouldBe Set("italian", "english")
  }

  it should "find dictionaries by language" in {
    val italianDicts = repo.getDictionariesByLanguage("italian")
    italianDicts.size shouldBe 2
    italianDicts.map(_.name).toSet shouldBe Set("1k", "10k")
    italianDicts.foreach(_.language shouldBe "italian")

    val englishDicts = repo.getDictionariesByLanguage("english")
    englishDicts.size shouldBe 1
    englishDicts.head.name shouldBe "1k"
    englishDicts.head.language shouldBe "english"

    val nonExistentDicts = repo.getDictionariesByLanguage("spanish")
    nonExistentDicts shouldBe empty
  }

  it should "find a dictionary by name" in {
    val dict = repo.getDictionaryByName("1k")
    dict should not be None
    dict.map(_.name) shouldBe Some("1k")

    val dict10k = repo.getDictionaryByName("10k")
    dict10k should not be None
    dict10k.map(_.name) shouldBe Some("10k")
    dict10k.map(_.language) shouldBe Some("italian")

    val nonExistentDict = repo.getDictionaryByName("nonexistent")
    nonExistentDict shouldBe None
  }

  it should "handle non-existent directories gracefully" in {
    // Clean up the directory for this specific test
    afterEach()

    val nonExistentDir = new File(tempDir, "nonexistent").getAbsolutePath
    val emptyRepo = new FileDictionaryRepository(nonExistentDir)

    emptyRepo.getAllDictionaries shouldBe empty
    emptyRepo.getDictionariesByLanguage("any") shouldBe empty
    emptyRepo.getDictionaryByName("any") shouldBe None
  }
