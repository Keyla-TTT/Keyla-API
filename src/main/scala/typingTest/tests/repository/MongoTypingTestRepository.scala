package typingTest.tests.repository

import com.mongodb.client.{MongoClients, MongoCollection, MongoDatabase}
import org.bson.Document
import org.bson.types.ObjectId
import typingTest.tests.model.{
  CompletedInfo,
  DefaultContext,
  PersistedTypingTest,
  TypingTest
}
import typingTest.dictionary.model.Dictionary
import users_management.repository.DatabaseInfos
import com.github.nscala_time.time.Imports.DateTime
import scala.jdk.CollectionConverters.*

class MongoTypingTestRepository(dbInfos: DatabaseInfos)
    extends TypingTestRepository:

  private val mongoClient = MongoClients.create(dbInfos.mongoUri)
  private val database: MongoDatabase =
    mongoClient.getDatabase(dbInfos.databaseName)
  private val collection: MongoCollection[Document] =
    database.getCollection(dbInfos.collectionName)

  private def toDocument(test: PersistedTypingTest): Document =
    val testData = test.testData
    val sourcesDoc = testData.sources.map { source =>
      new Document()
        .append("name", source.name)
        .append("language", source.language)
        .append("filePath", source.filePath)
    }.asJava

    val completedInfoDoc = new Document()
      .append("completed", testData.info.completed)
      .append(
        "completedDateTime",
        testData.info.completedDateTime.map(_.toString).orNull
      )

    val testDataDoc = new Document()
      .append("sources", sourcesDoc)
      .append("modifiers", testData.modifiers.asJava)
      .append("info", completedInfoDoc)
      .append("words", testData.words.asJava)

    new Document()
      .append("profileId", test.profileId)
      .append("testData", testDataDoc)
      .append("createdAt", test.createdAt.toString)
      .append("language", test.language)
      .append("wordCount", test.wordCount)
      .append("completedAt", test.completedAt.map(_.toString).orNull)
      .append("accuracy", test.accuracy.map(Double.box).orNull)
      .append("rawAccuracy", test.rawAccuracy.map(Double.box).orNull)
      .append("testTime", test.testTime.map(Long.box).orNull)
      .append("errorCount", test.errorCount.map(Int.box).orNull)
      .append("errorWordIndices", test.errorWordIndices.map(_.asJava).orNull)
      .append("timeLimit", test.timeLimit.map(Long.box).orNull)

  private def fromDocument(doc: Document): PersistedTypingTest =
    val testDataDoc = doc.get("testData", classOf[Document])

    val sourcesDoc = testDataDoc.getList("sources", classOf[Document]).asScala
    val sources = sourcesDoc.map { sourceDoc =>
      Dictionary(
        name = sourceDoc.getString("name"),
        language = sourceDoc.getString("language"),
        filePath = sourceDoc.getString("filePath")
      )
    }.toSet

    val completedInfoDoc = testDataDoc.get("info", classOf[Document])
    val completedDateTime =
      Option(completedInfoDoc.getString("completedDateTime"))
        .map(DateTime.parse)
    val completedInfo = CompletedInfo(
      completedInfoDoc.getBoolean("completed"),
      completedDateTime
    )

    val modifiers =
      testDataDoc.getList("modifiers", classOf[String]).asScala.toSeq
    val words = testDataDoc.getList("words", classOf[String]).asScala.toSeq

    val testData = TypingTest(sources, modifiers, completedInfo, words)

    val completedAt = Option(doc.getString("completedAt")).map(DateTime.parse)
    val accuracy = Option(doc.getDouble("accuracy")).map(_.doubleValue)
    val rawAccuracy = Option(doc.getDouble("rawAccuracy")).map(_.doubleValue)
    val testTime = Option(doc.getLong("testTime")).map(_.longValue)
    val errorCount = Option(doc.getInteger("errorCount")).map(_.intValue)
    val errorWordIndices =
      Option(doc.getList("errorWordIndices", classOf[Integer]))
        .map(_.asScala.map(_.intValue).toList)
    val timeLimit = Option(doc.getLong("timeLimit")).map(_.longValue)

    PersistedTypingTest(
      id = Some(doc.getObjectId("_id").toString),
      profileId = doc.getString("profileId"),
      testData = testData,
      createdAt = DateTime.parse(doc.getString("createdAt")),
      language = doc.getString("language"),
      wordCount = doc.getInteger("wordCount"),
      completedAt = completedAt,
      accuracy = accuracy,
      rawAccuracy = rawAccuracy,
      testTime = testTime,
      errorCount = errorCount,
      errorWordIndices = errorWordIndices,
      timeLimit = timeLimit
    )

  override def get(id: String): Option[PersistedTypingTest] =
    try
      Option(collection.find(new Document("_id", new ObjectId(id))).first())
        .map(fromDocument)
    catch case _: Exception => None

  override def create(test: PersistedTypingTest): PersistedTypingTest =
    val doc = toDocument(test)
    collection.insertOne(doc)
    val generatedId = doc.getObjectId("_id")
    test.copy(id = Some(generatedId.toString))

  override def update(test: PersistedTypingTest): Option[PersistedTypingTest] =
    test.id.flatMap { id =>
      try
        val updateDoc = new Document("$set", toDocument(test.copy(id = None)))
        val result = collection.updateOne(
          new Document("_id", new ObjectId(id)),
          updateDoc
        )
        if result.getModifiedCount > 0 then Some(test) else None
      catch case _: Exception => None
    }

  override def delete(id: String): Boolean =
    try
      val result = collection.deleteOne(new Document("_id", new ObjectId(id)))
      result.getDeletedCount > 0
    catch case _: Exception => false

  override def deleteAll(): Boolean =
    val hasTests = list().nonEmpty
    if hasTests then
      collection.drop()
      true
    else false

  override def list(): List[PersistedTypingTest] =
    try
      collection
        .find()
        .into(new java.util.ArrayList[Document]())
        .asScala
        .map(fromDocument)
        .toList
    catch case _: Exception => List()

  override def getByProfileId(profileId: String): List[PersistedTypingTest] =
    try
      collection
        .find(new Document("profileId", profileId))
        .into(new java.util.ArrayList[Document]())
        .asScala
        .map(fromDocument)
        .toList
    catch case _: Exception => List()

  override def getByLanguage(language: String): List[PersistedTypingTest] =
    try
      collection
        .find(new Document("language", language))
        .into(new java.util.ArrayList[Document]())
        .asScala
        .map(fromDocument)
        .toList
    catch case _: Exception => List()

  override def getByProfileIdAndLanguage(
      profileId: String,
      language: String
  ): List[PersistedTypingTest] =
    try
      val filter = new Document()
        .append("profileId", profileId)
        .append("language", language)
      collection
        .find(filter)
        .into(new java.util.ArrayList[Document]())
        .asScala
        .map(fromDocument)
        .toList
    catch case _: Exception => List()

  override def getLastNonCompletedByProfileId(
      profileId: String
  ): Option[PersistedTypingTest] =
    try
      val filter = new Document()
        .append("profileId", profileId)
        .append("completedAt", null)
      Option(
        collection
          .find(filter)
          .sort(new Document("createdAt", -1))
          .limit(1)
          .first()
      )
        .map(fromDocument)
    catch case _: Exception => None

  override def deleteNonCompletedByProfileId(profileId: String): Int =
    try
      val filter = new Document()
        .append("profileId", profileId)
        .append("completedAt", null)
      val result = collection.deleteMany(filter)
      result.getDeletedCount.toInt
    catch case _: Exception => 0

  override def getCompletedById(id: String): Option[PersistedTypingTest] =
    try
      val filter = new Document()
        .append("_id", new ObjectId(id))
        .append("completedAt", new Document("$ne", null))
      Option(collection.find(filter).first())
        .map(fromDocument)
    catch case _: Exception => None

  def close(): Unit =
    mongoClient.close()
