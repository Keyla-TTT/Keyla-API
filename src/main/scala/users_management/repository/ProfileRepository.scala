package users_management.repository

import users_management.model.Profile

trait ProfileRepository:

  def get(id: String): Option[Profile]
  def create(profile: Profile): Profile
  def update(profile: Profile): Option[Profile]
  def delete(id: String): Boolean
  def list(): List[Profile]
   
