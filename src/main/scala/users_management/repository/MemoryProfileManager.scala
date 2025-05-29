package users_management.repository

import users_management.model.{Profile, ProfileManager, UserProfile}

import java.util.UUID

/**
 * Implementazione in memoria del gestore di profili utente.
 * Utilizza una mappa per memorizzare i profili in memoria.
 */
case class MemoryProfileManager() extends ProfileManager:
  // Mappa che associa gli UUID dei profili agli oggetti Profile.
  private var profiles: Map[UUID, Profile] = Map()

  /**
   * Recupera un profilo dato il suo ID.
   * @param id L'UUID del profilo da recuperare.
   * @return Un'opzione contenente il profilo se trovato, altrimenti None.
   */
  def getProfile(id: UUID): Option[Profile] =
    profiles.get(id)

  /**
   * Crea un nuovo profilo e lo aggiunge alla mappa in memoria.
   * @param name Il nome del profilo.
   * @param email L'email associata al profilo.
   * @param password La password del profilo.
   * @param settings Le impostazioni associate al profilo.
   * @return Il profilo creato.
   */
  def createProfile(name: String, email: String, password: String, settings: Set[String]): Profile =
    val profile = UserProfile(UUID.randomUUID(), name, email, password, settings)
    profiles += (profile.getId -> profile)
    profile

  /**
   * Aggiorna un profilo esistente con i nuovi valori forniti.
   * @param id L'UUID del profilo da aggiornare.
   * @param name Il nuovo nome del profilo (opzionale).
   * @param email La nuova email del profilo (opzionale).
   * @param password La nuova password del profilo (opzionale).
   * @param settings Le nuove impostazioni del profilo (opzionale).
   * @return Un'opzione contenente il profilo aggiornato se trovato, altrimenti None.
   */
  def updateProfile(id: UUID, name: Option[String], email: Option[String], password: Option[String], settings: Option[Set[String]]): Option[Profile] =
    profiles.get(id).map : profile =>
      val updatedProfile = profile.copy(
        name = name.getOrElse(profile.getName),
        email = email.getOrElse(profile.getEmail),
        password = password.getOrElse(profile.getPassword),
        settings = settings.getOrElse(profile.getSettings)
      )
      profiles += (updatedProfile.getId -> updatedProfile)
      updatedProfile

  /**
   * Elimina un profilo dato il suo ID.
   * @param id L'UUID del profilo da eliminare.
   * @return True se il profilo è stato eliminato con successo, altrimenti False.
   */
  def deleteProfile(id: UUID): Boolean =
    if (profiles.contains(id))
      profiles -= id
      true
    else
      false

  /**
   * Restituisce una lista di tutti i profili memorizzati.
   * @return Una lista contenente tutti i profili.
   */
  def listProfiles(): List[Profile] =
    profiles.values.toList