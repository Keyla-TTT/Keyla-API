package users_management.repository

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import users_management.repository.InMemoryProfileRepository

import users_management.model.UserProfile

class TestInMemoryRepo extends AnyFunSuite, Matchers:

  val repo = InMemoryProfileRepository()
  val profile1 = UserProfile(Some("1"), "Mario", "mario@email.com", "pass", Set("a"))
  val profile2 = UserProfile(Some("2"), "Luigi", "luigi@email.com", "pass2", Set("b"))

  test("create aggiunge un nuovo profilo e lo restituisce") {
    repo.create(profile1) shouldBe profile1
    repo.get("1") shouldBe Some(profile1)
  }

  test("get restituisce None se l'id non esiste") {
    repo.get("not-exist") shouldBe None
  }

  test("update aggiorna un profilo esistente e restituisce il profilo aggiornato") {
    repo.create(profile1)
    val updated = profile1.copy(name = "Mario Updated")
    repo.update(updated) shouldBe Some(updated)
    repo.get("1") shouldBe Some(updated)
  }

  test("update restituisce None se il profilo non esiste") {
    repo.update(profile2) shouldBe None
  }

  test("delete rimuove un profilo esistente e restituisce true") {
    repo.create(profile2)
    repo.delete("2") shouldBe true
    repo.get("2") shouldBe None
  }

  test("delete restituisce false se il profilo non esiste") {
    repo.delete("not-exist") shouldBe false
  }

  test("list restituisce tutti i profili presenti") {
    repo.create(profile1)
    repo.create(profile2)
    repo.list().toSet shouldBe Set(profile1, profile2)
  }
