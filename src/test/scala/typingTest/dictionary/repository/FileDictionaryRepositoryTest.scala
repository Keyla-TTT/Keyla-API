package typingTest.dictionary.repository

import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
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
    Files.write(
      new File(italianDir, "italian_1k.txt").toPath,
      "italian1k".getBytes
    )
    Files.write(
      new File(italianDir, "italian_10k.txt").toPath,
      "italian10k".getBytes
    )
    Files.write(
      new File(englishDir, "english_1k.txt").toPath,
      "english1k".getBytes
    )
    Files.write(
      new File(englishDir, "english_10k.txt").toPath,
      "english10k".getBytes
    )

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

    dictionaries.size shouldBe 4
    dictionaries.map(_.name).toSet shouldBe Set(
      "italian_1k",
      "italian_10k",
      "english_1k",
      "english_10k"
    )
    dictionaries.map(_.language).toSet shouldBe Set("italian", "english")
  }

  it should "find dictionaries by language" in {
    val italianDicts = repo.getDictionariesByLanguage("italian")
    italianDicts.size shouldBe 2
    italianDicts.map(_.name).toSet shouldBe Set("italian_1k", "italian_10k")
    italianDicts.foreach(_.language shouldBe "italian")

    val englishDicts = repo.getDictionariesByLanguage("english")
    englishDicts.size shouldBe 2
    englishDicts.map(_.name).toSet shouldBe Set("english_1k", "english_10k")
    englishDicts.foreach(_.language shouldBe "english")

    val nonExistentDicts = repo.getDictionariesByLanguage("spanish")
    nonExistentDicts shouldBe empty
  }

  it should "find a dictionary by name" in {
    val dict = repo.getDictionaryByName("italian_1k")
    dict should not be None
    dict.map(_.name) shouldBe Some("italian_1k")
    dict.map(_.language) shouldBe Some("italian")

    val dict10k = repo.getDictionaryByName("english_10k")
    dict10k should not be None
    dict10k.map(_.name) shouldBe Some("english_10k")
    dict10k.map(_.language) shouldBe Some("english")

    val nonExistentDict = repo.getDictionaryByName("nonexistent")
    nonExistentDict shouldBe None
  }

  it should "find a dictionary by language and name" in {
    val italianDict =
      repo.getDictionaryByLanguageAndName("italian", "italian_1k")
    italianDict should not be None
    italianDict.map(_.name) shouldBe Some("italian_1k")
    italianDict.map(_.language) shouldBe Some("italian")

    val englishDict =
      repo.getDictionaryByLanguageAndName("english", "english_10k")
    englishDict should not be None
    englishDict.map(_.name) shouldBe Some("english_10k")
    englishDict.map(_.language) shouldBe Some("english")

    val nonExistentDict =
      repo.getDictionaryByLanguageAndName("italian", "nonexistent")
    nonExistentDict shouldBe None

    val wrongLanguage =
      repo.getDictionaryByLanguageAndName("spanish", "italian_1k")
    wrongLanguage shouldBe None
  }

  it should "handle non-existent directories gracefully" in {
    // Clean up the directory for this specific test
    afterEach()

    val nonExistentDir = new File(tempDir, "nonexistent").getAbsolutePath
    val emptyRepo = new FileDictionaryRepository(nonExistentDir)

    emptyRepo.getAllDictionaries shouldBe empty
    emptyRepo.getDictionariesByLanguage("any") shouldBe empty
    emptyRepo.getDictionaryByName("any") shouldBe None
    emptyRepo.getDictionaryByLanguageAndName("any", "any") shouldBe None
  }
