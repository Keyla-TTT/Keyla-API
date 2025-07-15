package users_management.repository

import com.mongodb.client.{MongoClients, MongoCollection, MongoDatabase}
import org.bson.Document
import org.bson.types.ObjectId
import users_management.model.{Profile, UserProfile}

import scala.jdk.CollectionConverters.*
import scala.util.Try

class MongoProfileRepository(dbInfos: DatabaseInfos)
    extends ProfileRepository
    with AutoCloseable:

  private val mongoClient = MongoClients.create(dbInfos.mongoUri)
  private val database: MongoDatabase =
    mongoClient.getDatabase(dbInfos.databaseName)
  private val collection: MongoCollection[Document] =
    database.getCollection(dbInfos.collectionName)

  private def toDocument(profile: Profile): Document =
    new Document()
      .append("name", profile.name)
      .append("email", profile.email)
      .append("settings", profile.settings.toList.asJava)

  private def fromDocument(doc: Document): Profile =
    UserProfile(
      id = Some(doc.getObjectId("_id").toString),
      name = doc.getString("name"),
      email = doc.getString("email"),
      settings = doc.getList("settings", classOf[String]).asScala.toSet
    )

  override def get(id: String): Option[Profile] =
    try
      Option(collection.find(new Document("_id", new ObjectId(id))).first())
        .map(fromDocument)
    catch case _: Exception => None

  override def create(profile: Profile): Profile =
    val doc = toDocument(profile)
    collection.insertOne(doc)
    val generatedId = doc.getObjectId("_id")
    fromDocument(doc.append("_id", generatedId))

  override def update(profile: Profile): Option[Profile] =
    profile.id.flatMap { id =>
      try
        val updateDoc = new Document(
          "$set",
          new Document()
            .append("name", profile.name)
            .append("email", profile.email)
            .append("settings", profile.settings.toList.asJava)
        )
        val result = collection.updateOne(
          new Document("_id", new ObjectId(id)),
          updateDoc
        )
        if result.getModifiedCount > 0 then Some(profile) else None
      catch case _: Exception => None
    }

  override def delete(id: String): Boolean =
    try
      val result = collection.deleteOne(new Document("_id", new ObjectId(id)))
      result.getDeletedCount > 0
    catch case _: Exception => false

  override def deleteAll(): Boolean =
    val hasProfiles = list().nonEmpty
    if hasProfiles then
      collection.drop()
      true
    else false

  override def list(): List[Profile] =
    try
      collection
        .find()
        .into(new java.util.ArrayList[Document]())
        .asScala
        .map(fromDocument)
        .toList
    catch case _: Exception => List()

  override def close(): Unit =
    Try(mongoClient.close())
