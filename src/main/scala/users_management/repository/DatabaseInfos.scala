package users_management.repository

/** Case class representing MongoDB database connection and collection information.
 *
 * This class encapsulates the necessary information to connect to and use a MongoDB database,
 * including the collection name, connection URI and database name.
 *
 * @param collectionName The name of the MongoDB collection to use
 * @param mongoUri       The MongoDB connection URI string (e.g. mongodb://host:port)
 * @param databaseName   The name of the MongoDB database to use
 */
case class DatabaseInfos(
                          collectionName: String,
                          mongoUri: String,
                          databaseName: String
                        )