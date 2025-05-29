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
  def getId: UUID

  /**
   * Ottiene il nome del profilo.
   * @return Nome del profilo.
   */
  def getName: String

  /**
   * Ottiene l'email associata al profilo.
   * @return Email del profilo.
   */
  def getEmail: String

  /**
   * Ottiene la password associata al profilo.
   * @return Password del profilo.
   */
  def getPassword: String

  /**
   * Ottiene le impostazioni associate al profilo.
   * @return Set di impostazioni del profilo.
   */
  def getSettings: Set[String]

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
    (id: UUID = this.getId,
     name: String = this.getName,
     email: String = this.getEmail,
     password: String = this.getPassword,
     settings: Set[String] = this.getSettings): Profile

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
  id: UUID,
  name: String,
  email: String,
  password: String,
  settings: Set[String]
) extends Profile:
  /**
   * @inheritdoc
   */
  def getId: UUID = id

  /**
   * @inheritdoc
   */
  def getName: String = name

  /**
   * @inheritdoc
   */
  def getEmail: String = email

  /**
   * @inheritdoc
   */
  def getPassword: String = password

  /**
   * @inheritdoc
   */
  def getSettings: Set[String] = settings

  /**
   * Crea una copia del profilo utente con proprietà opzionalmente modificate.
   * @param id Nuovo ID del profilo (opzionale).
   * @param name Nuovo nome del profilo (opzionale).
   * @param email Nuova email del profilo (opzionale).
   * @param password Nuova password del profilo (opzionale).
   * @param settings Nuove impostazioni del profilo (opzionale).
   * @return Una nuova istanza di UserProfile con le modifiche applicate.
   */
  def copy(
    id: UUID = this.id,
    name: String = this.name,
    email: String = this.email,
    password: String = this.password,
    settings: Set[String] = this.settings
  ): UserProfile =
    UserProfile(id, name, email, password, settings)