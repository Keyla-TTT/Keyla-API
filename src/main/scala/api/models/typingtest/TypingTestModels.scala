package api.models.typingtest

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import typingTest.tests.model.{DefaultContext, PersistedTypingTest, TypingTest}

case class SourceWithMerger(
    name: String,
    merger: Option[String] = None
)

case class TestRequest(
    profileId: String,
    sources: List[SourceWithMerger],
    wordCount: Int,
    modifiers: List[String] = List.empty,
    timeLimit: Option[Long] = None
)

case class TestResultsRequest(
    accuracy: Double,
    rawAccuracy: Double,
    testTime: Long,
    errorCount: Int,
    errorWordIndices: List[Int] = List.empty
)

case class TestResponse(
    testId: String,
    profileId: String,
    words: List[String],
    sources: List[SourceWithMerger],
    modifiers: List[String],
    createdAt: String,
    completedAt: Option[String] = None
)

case class TestListResponse(
    tests: List[TestResponse]
)

case class LastTestResponse(
    words: List[String]
)

case class DictionaryInfo(
    name: String
)

case class DictionariesResponse(
    dictionaries: List[DictionaryInfo]
)

case class ModifierInfo(
    name: String,
    description: String
)

case class ModifiersResponse(
    modifiers: List[ModifierInfo]
)

case class MergerInfo(
    name: String,
    description: String
)

case class MergersResponse(
    mergers: List[MergerInfo]
)

object TypingTestModels:
  given JsonValueCodec[SourceWithMerger] = JsonCodecMaker.make
  given JsonValueCodec[TestRequest] = JsonCodecMaker.make
  given JsonValueCodec[TestResultsRequest] = JsonCodecMaker.make
  given JsonValueCodec[TestResponse] = JsonCodecMaker.make
  given JsonValueCodec[TestListResponse] = JsonCodecMaker.make
  given JsonValueCodec[LastTestResponse] = JsonCodecMaker.make
  given JsonValueCodec[DictionaryInfo] = JsonCodecMaker.make
  given JsonValueCodec[DictionariesResponse] = JsonCodecMaker.make
  given JsonValueCodec[ModifierInfo] = JsonCodecMaker.make
  given JsonValueCodec[ModifiersResponse] = JsonCodecMaker.make
  given JsonValueCodec[MergerInfo] = JsonCodecMaker.make
  given JsonValueCodec[MergersResponse] = JsonCodecMaker.make

  def testToResponse(
      test: TypingTest[String] & DefaultContext,
      testId: String,
      profileId: String
  ): TestResponse =
    TestResponse(
      testId = testId,
      profileId = profileId,
      words = test.words.toList,
      sources = test.sources.map(d => SourceWithMerger(d.name)).toList,
      modifiers = test.modifiers.toList,
      createdAt = java.time.Instant.now().toString,
      completedAt = test.info.completedDateTime.map(_.toString)
    )

  def persistedTestToResponse(
      persistedTest: PersistedTypingTest
  ): TestResponse =
    TestResponse(
      testId = persistedTest.id.getOrElse(""),
      profileId = persistedTest.profileId,
      words = persistedTest.testData.words.toList,
      sources = persistedTest.testData.sources
        .map(d => SourceWithMerger(d.name))
        .toList,
      modifiers = persistedTest.testData.modifiers.toList,
      createdAt = persistedTest.createdAt.toString,
      completedAt =
        persistedTest.testData.info.completedDateTime.map(_.toString)
    )

  def persistedTestToLastTestResponse(
      persistedTest: PersistedTypingTest
  ): LastTestResponse =
    LastTestResponse(
      words = persistedTest.testData.words.toList
    )

  def testListToResponse(tests: List[PersistedTypingTest]): TestListResponse =
    TestListResponse(tests.map(persistedTestToResponse))

  def dictionariesToResponse(
      dictionaries: Seq[typingTest.dictionary.model.Dictionary]
  ): DictionariesResponse =
    DictionariesResponse(
      dictionaries.map(dict => DictionaryInfo(dict.name)).toList
    )

  def modifiersToResponse(modifiers: Set[String]): ModifiersResponse =
    ModifiersResponse(
      modifiers
        .map(name =>
          ModifierInfo(name, s"Applies $name transformation to words")
        )
        .toList
    )

  def mergersToResponse(mergers: Set[String]): MergersResponse =
    MergersResponse(
      mergers
        .map(name => MergerInfo(name, s"Combines sources using $name strategy"))
        .toList
    )
