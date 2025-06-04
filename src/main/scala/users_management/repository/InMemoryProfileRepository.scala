package users_management.repository

import users_management.model.Profile
import scala.collection.mutable


class InMemoryProfileRepository extends ProfileRepository:

  // Mappa per memorizzare i profili, con l'ID come chiave.
  private val profiles: mutable.Map[String, Profile] = mutable.Map()
  
  override def get(id: String): Option[Profile] =
    profiles.get(id)
  
  override def create(profile: Profile): Profile =
    val id = generateUniqueId()
    val profileWithId = profile.copy(id = Some(id))
    profiles += (id -> profileWithId)
    profileWithId

  private def generateUniqueId(): String =
    val id = java.util.UUID.randomUUID().toString
    id match
      case id if profiles.contains(id) => generateUniqueId() 
      case _ => id
  
  override def update(profile: Profile): Option[Profile] =
    profile.id.flatMap: id =>
      if profiles.contains(id) then
        profiles.update(id, profile)
        Some(profile)
      else None
  
  override def delete(id: String): Boolean =
    profiles.remove(id).isDefined
  
  override def list(): List[Profile] =
    profiles.values.toList