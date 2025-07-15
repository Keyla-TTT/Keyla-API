package analytics.repository

import analytics.model.{Statistics, UserStatistics}
import analytics.repository.DatabaseInfos
import com.mongodb.client.{MongoClients, MongoCollection, MongoDatabase}
import org.bson.Document
import org.bson.types.ObjectId

import scala.jdk.CollectionConverters.*
import scala.util.Try

class MongoStatisticsRepository(dbInfos: DatabaseInfos)
    extends StatisticsRepository
    with AutoCloseable:

  private val mongoClient = MongoClients.create(dbInfos.mongoUri)
  private val database: MongoDatabase =
    mongoClient.getDatabase(dbInfos.databaseName)
  private val collection: MongoCollection[Document] =
    database.getCollection(dbInfos.collectionName)

  override def get(testId: String): Option[Statistics] =
    Try {
      Option(
        collection
          .find(new Document("_id", new ObjectId(testId)))
          .first()
      ).map(fromDocument)
    }.getOrElse(None)

  override def save(statistics: Statistics): Statistics =
    val doc = toDocument(statistics)
    collection.insertOne(doc)
    val generatedId = doc.getObjectId("_id")
    fromDocument(doc.append("_id", generatedId))

  private def toDocument(statistics: Statistics): Document =
    new Document()
      .append("userID", statistics.userId)
      .append("wpm", statistics.wpm)
      .append("accuracy", statistics.accuracy)
      .append("errors", statistics.errors.asJava)
      .append("timestamp", statistics.timestamp)

  override def deleteAll(userId: String): Boolean =
    Try {
      val result = collection.deleteMany(new Document("userID", userId))
      result.getDeletedCount > 0
    }.getOrElse(false)

  override def list(userId: String): List[Statistics] =
    Try {
      collection
        .find(new Document("userID", userId))
        .into(new java.util.ArrayList[Document]())
        .asScala
        .map(fromDocument)
        .toList
    }.getOrElse(List())

  private def fromDocument(doc: Document): Statistics =
    UserStatistics(
      userId = doc.getString("userID"),
      testId = doc.getObjectId("_id").toString,
      wpm = doc.getDouble("wpm"),
      accuracy = doc.getDouble("accuracy"),
      errors = doc
        .getList("errors", classOf[Integer])
        .asScala
        .map(_.intValue())
        .toList,
      timestamp = doc.getLong("timestamp")
    )

  override def clean(): Boolean =
    Try {
      val result = collection.deleteMany(new Document())
      result.getDeletedCount > 0
    }.getOrElse(false)

  override def close(): Unit =
    Try(mongoClient.close())
