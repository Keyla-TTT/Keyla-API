package api.models

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import config.{
  ConfigKey,
  ConfigEntry,
  ConfigListResponse,
  ConfigUpdateRequest,
  ConfigUpdateResponse,
  AppConfig
}
import users_management.model.Profile
import typingTest.tests.model.{DefaultContext, PersistedTypingTest, TypingTest}

case class CreateProfileRequest(
    name: String,
    email: String,
    settings: Set[String] = Set.empty
)

case class ProfileResponse(
    id: String,
    name: String,
    email: String,
    settings: Set[String]
)

case class ProfileListResponse(
    profiles: List[ProfileResponse]
)

case class TestRequest(
    profileId: String,
    language: String,
    dictionaryName: String,
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
    language: String,
    dictionaryName: String,
    modifiers: List[String],
    sources: List[String],
    createdAt: String,
    completedAt: Option[String] = None,
    accuracy: Option[Double] = None,
    rawAccuracy: Option[Double] = None,
    testTime: Option[Long] = None,
    errorCount: Option[Int] = None,
    errorWordIndices: Option[List[Int]] = None,
    timeLimit: Option[Long] = None
)

case class TestListResponse(
    tests: List[TestResponse]
)

case class ErrorResponse(
    message: String,
    code: String,
    statusCode: Int
)

case class LastTestResponse(
    words: List[String],
    timeLimit: Option[Long] = None
)

case class DictionaryInfo(
    name: String,
    language: String
)

case class DictionariesResponse(
    dictionaries: List[DictionaryInfo]
)

case class LanguageInfo(
    language: String,
    dictionaries: List[String]
)

case class LanguagesResponse(
    languages: List[LanguageInfo]
)

object ApiModels:
  given JsonValueCodec[CreateProfileRequest] = JsonCodecMaker.make
  given JsonValueCodec[ProfileResponse] = JsonCodecMaker.make
  given JsonValueCodec[ProfileListResponse] = JsonCodecMaker.make
  given JsonValueCodec[TestRequest] = JsonCodecMaker.make
  given JsonValueCodec[TestResultsRequest] = JsonCodecMaker.make
  given JsonValueCodec[TestResponse] = JsonCodecMaker.make
  given JsonValueCodec[TestListResponse] = JsonCodecMaker.make
  given JsonValueCodec[LastTestResponse] = JsonCodecMaker.make
  given JsonValueCodec[ErrorResponse] = JsonCodecMaker.make
  given JsonValueCodec[DictionaryInfo] = JsonCodecMaker.make
  given JsonValueCodec[DictionariesResponse] = JsonCodecMaker.make
  given JsonValueCodec[LanguageInfo] = JsonCodecMaker.make
  given JsonValueCodec[LanguagesResponse] = JsonCodecMaker.make

  // Configuration codecs
  given JsonValueCodec[ConfigKey] = JsonCodecMaker.make
  given JsonValueCodec[ConfigEntry] = JsonCodecMaker.make
  given JsonValueCodec[ConfigListResponse] = JsonCodecMaker.make
  given JsonValueCodec[ConfigUpdateRequest] = JsonCodecMaker.make
  given JsonValueCodec[ConfigUpdateResponse] = JsonCodecMaker.make

  def profileToResponse(profile: Profile): ProfileResponse =
    ProfileResponse(
      id = profile.id.getOrElse(""),
      name = profile.name,
      email = profile.email,
      settings = profile.settings
    )

  def profileListToResponse(profiles: List[Profile]): ProfileListResponse =
    ProfileListResponse(profiles.map(profileToResponse))

  def testToResponse(
      test: TypingTest[String] & DefaultContext,
      testId: String,
      profileId: String,
      timeLimit: Option[Long] = None
  ): TestResponse =
    TestResponse(
      testId = testId,
      profileId = profileId,
      words = test.words.toList,
      language = test.sources.headOption.map(_.language).getOrElse("english"),
      dictionaryName = test.sources.headOption.map(_.name).getOrElse("unknown"),
      modifiers = test.modifiers.toList,
      sources = test.sources.map(_.name).toList,
      createdAt = java.time.Instant.now().toString,
      completedAt = None,
      timeLimit = timeLimit
    )

  def persistedTestToResponse(
      persistedTest: PersistedTypingTest
  ): TestResponse =
    TestResponse(
      testId = persistedTest.id.getOrElse(""),
      profileId = persistedTest.profileId,
      words = persistedTest.testData.words.toList,
      language = persistedTest.language,
      dictionaryName = persistedTest.testData.sources.headOption
        .map(_.name)
        .getOrElse("unknown"),
      modifiers = persistedTest.testData.modifiers.toList,
      sources = persistedTest.testData.sources.map(_.name).toList,
      createdAt = persistedTest.createdAt.toString,
      completedAt = persistedTest.completedAt.map(_.toString),
      accuracy = persistedTest.accuracy,
      rawAccuracy = persistedTest.rawAccuracy,
      testTime = persistedTest.testTime,
      errorCount = persistedTest.errorCount,
      errorWordIndices = persistedTest.errorWordIndices,
      timeLimit = persistedTest.timeLimit
    )

  def persistedTestToLastTestResponse(
      persistedTest: PersistedTypingTest
  ): LastTestResponse =
    LastTestResponse(
      words = persistedTest.testData.words.toList,
      timeLimit = persistedTest.timeLimit
    )

  def testListToResponse(tests: List[PersistedTypingTest]): TestListResponse =
    TestListResponse(tests.map(persistedTestToResponse))

  def dictionariesToResponse(
      dictionaries: Seq[typingTest.dictionary.model.Dictionary]
  ): DictionariesResponse =
    DictionariesResponse(
      dictionaries.map(dict => DictionaryInfo(dict.name, dict.language)).toList
    )

  def languagesToResponse(
      dictionaries: Seq[typingTest.dictionary.model.Dictionary]
  ): LanguagesResponse =
    val groupedByLanguage = dictionaries.groupBy(_.language)
    val languages = groupedByLanguage
      .map { case (language, dicts) =>
        LanguageInfo(language, dicts.map(_.name).toList.sorted)
      }
      .toList
      .sortBy(_.language)
    LanguagesResponse(languages)
