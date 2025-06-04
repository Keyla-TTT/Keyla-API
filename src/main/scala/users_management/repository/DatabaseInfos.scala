package users_management.repository

case class DatabaseInfos(
                          collectionName: String,
                          mongoUri: String,
                          databaseName: String
                        )