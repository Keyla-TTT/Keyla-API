package users_management.model
import java.util.UUID

/**
 * Trait che rappresenta un profilo utente.
 * Fornisce metodi per ottenere le proprietà del profilo e un metodo per creare una copia modificata del profilo.
 */
trait Profile:
  /**
   * Ottiene l'ID univoco del profilo.
   * @return UUID del profilo.
   */
  def id: UUID

  /**
   * Ottiene il nome del profilo.
   * @return Nome del profilo.
   */
  def name: String

  /**
   * Ottiene l'email associata al profilo.
   * @return Email del profilo.
   */
  def email: String

  /**
   * Ottiene la password associata al profilo.
   * @return Password del profilo.
   */
  def password: String

  /**
   * Ottiene le impostazioni associate al profilo.
   * @return Set di impostazioni del profilo.
   */
  def settings: Set[String]

  /**
   * Crea una copia del profilo con proprietà opzionalmente modificate.
   * @param id Nuovo ID del profilo (opzionale).
   * @param name Nuovo nome del profilo (opzionale).
   * @param email Nuova email del profilo (opzionale).
   * @param password Nuova password del profilo (opzionale).
   * @param settings Nuove impostazioni del profilo (opzionale).
   * @return Una nuova istanza di Profile con le modifiche applicate.
   */
  def copy
    (id: UUID = this.id,
     name: String = this.name,
     email: String = this.email,
     password: String = this.password,
     settings: Set[String] = this.settings): Profile

/**
 * Implementazione concreta del trait Profile.
 * Rappresenta un profilo utente con proprietà immutabili.
 *
 * @param id ID univoco del profilo.
 * @param name Nome del profilo.
 * @param email Email associata al profilo.
 * @param password Password associata al profilo.
 * @param settings Set di impostazioni associate al profilo.
 */
case class UserProfile(
  override val id: UUID,
  override val name: String,
  override val email: String,
  override val password: String,
  override val settings: Set[String]
) extends Profile
