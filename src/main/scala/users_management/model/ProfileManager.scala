package users_management.model

import java.util.UUID

/**
 * Trait che rappresenta un gestore di profili utente.
 * Fornisce metodi per gestire la creazione, lettura, aggiornamento, eliminazione e 
 * elencazione dei profili utente.
 */
trait ProfileManager:
  /**
   * Recupera un profilo utente dato il suo ID.
   * @param id L'UUID del profilo da recuperare.
   * @return Un'opzione contenente il profilo se trovato, altrimenti None.
   */
  def getProfile(id: UUID): Option[Profile]

  /**
   * Crea un nuovo profilo utente.
   * @param name Il nome del profilo.
   * @param email L'email associata al profilo.
   * @param password La password del profilo.
   * @param settings Le impostazioni associate al profilo.
   * @return Il profilo creato.
   */
  def createProfile(name: String, email: String, password: String, settings: Set[String]): Profile

  /**
   * Aggiorna un profilo esistente.
   * @param id L'UUID del profilo da aggiornare.
   * @param name Il nuovo nome del profilo (opzionale).
   * @param email La nuova email del profilo (opzionale).
   * @param password La nuova password del profilo (opzionale).
   * @param settings Le nuove impostazioni del profilo (opzionale).
   * @return Un'opzione contenente il profilo aggiornato se trovato, altrimenti None.
   */
  def updateProfile(id: UUID, name: Option[String], email: Option[String], password: Option[String], settings: Option[Set[String]]): Option[Profile]

  /**
   * Elimina un profilo dato il suo ID.
   * @param id L'UUID del profilo da eliminare.
   * @return True se il profilo è stato eliminato con successo, altrimenti False.
   */
  def deleteProfile(id: UUID): Boolean

  /**
   * Elenca tutti i profili disponibili.
   * @return Una lista di tutti i profili.
   */
  def listProfiles(): List[Profile]

    
  
