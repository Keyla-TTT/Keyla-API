package typingTest.dictionary.repository

import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import typingTest.dictionary.model.{Dictionary, DictionaryJson}
import com.github.plokhotnyuk.jsoniter_scala.core.*

import java.io.File
import java.nio.file.{Files, Paths}
import scala.compiletime.uninitialized

class FileDictionaryRepositoryTest
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterEach:
  val tempDir: File = Files.createTempDirectory("dict-test").toFile
  var repo: FileDictionaryRepository = uninitialized

  override def beforeEach(): Unit =
    val italian1k =
      DictionaryJson("italian_1k", Seq("ciao", "mondo", "test", "scala"))
    val italian10k = DictionaryJson(
      "italian_10k",
      Seq("programmazione", "sviluppatore", "software")
    )
    val english1k =
      DictionaryJson("english_1k", Seq("hello", "world", "test", "scala"))
    val english10k =
      DictionaryJson("english_10k", Seq("programming", "developer", "software"))

    Files.write(
      new File(tempDir, "italian_1k.json").toPath,
      writeToString(italian1k).getBytes
    )
    Files.write(
      new File(tempDir, "italian_10k.json").toPath,
      writeToString(italian10k).getBytes
    )
    Files.write(
      new File(tempDir, "english_1k.json").toPath,
      writeToString(english1k).getBytes
    )
    Files.write(
      new File(tempDir, "english_10k.json").toPath,
      writeToString(english10k).getBytes
    )
    // Add a .txt dictionary
    Files.write(
      new File(tempDir, "french_1k.txt").toPath,
      "bonjour\nmonde\ntest\nscala".getBytes
    )

    repo = new FileDictionaryRepository(tempDir.getAbsolutePath)

  override def afterEach(): Unit =
    def deleteRecursively(file: File): Unit =
      if file.isDirectory then
        Option(file.listFiles()).foreach(_.foreach(deleteRecursively))
      file.delete()

    deleteRecursively(tempDir)
    tempDir.mkdirs()

  "FileDictionaryRepository" should "find all dictionaries" in {
    val dictionaries = repo.getAllDictionaries

    dictionaries.size shouldBe 5
    dictionaries.map(_.name).toSet shouldBe Set(
      "italian_1k",
      "italian_10k",
      "english_1k",
      "english_10k",
      "french_1k"
    )
  }

  it should "find a dictionary by name" in {
    val dict = repo.getDictionaryByName("italian_1k")
    dict should not be None
    dict.map(_.name) shouldBe Some("italian_1k")

    val dict10k = repo.getDictionaryByName("english_10k")
    dict10k should not be None
    dict10k.map(_.name) shouldBe Some("english_10k")

    val txtDict = repo.getDictionaryByName("french_1k")
    txtDict should not be None
    txtDict.map(_.name) shouldBe Some("french_1k")

    val nonExistentDict = repo.getDictionaryByName("nonexistent")
    nonExistentDict shouldBe None
  }

  it should "handle non-existent directories gracefully" in {
    afterEach()

    val nonExistentDir = new File(tempDir, "nonexistent").getAbsolutePath
    val emptyRepo = new FileDictionaryRepository(nonExistentDir)

    emptyRepo.getAllDictionaries shouldBe empty
    emptyRepo.getDictionaryByName("any") shouldBe None
  }

  it should "handle malformed JSON files gracefully" in {
    Files.write(
      new File(tempDir, "malformed.json").toPath,
      "{ invalid json }".getBytes
    )

    val dictionaries = repo.getAllDictionaries
    dictionaries.size shouldBe 5
    dictionaries.map(_.name).toSet shouldBe Set(
      "italian_1k",
      "italian_10k",
      "english_1k",
      "english_10k",
      "french_1k"
    )
  }
