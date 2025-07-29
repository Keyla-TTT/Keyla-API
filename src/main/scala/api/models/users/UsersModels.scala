package api.models.users

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import users_management.model.Profile

case class CreateProfileRequest(
    name: String,
    email: String,
    settings: Set[String] = Set.empty
)

case class ProfileResponse(
    id: String,
    name: String,
    email: String,
    settings: Set[String]
)

case class ProfileListResponse(
    profiles: List[ProfileResponse]
)

case class DeleteProfileResponse(
    success: Boolean,
    message: String
)

object UsersModels:
  given JsonValueCodec[CreateProfileRequest] = JsonCodecMaker.make
  given JsonValueCodec[ProfileResponse] = JsonCodecMaker.make
  given JsonValueCodec[ProfileListResponse] = JsonCodecMaker.make
  given JsonValueCodec[DeleteProfileResponse] = JsonCodecMaker.make

  def profileToResponse(profile: Profile): ProfileResponse =
    ProfileResponse(
      id = profile.id.getOrElse(""),
      name = profile.name,
      email = profile.email,
      settings = profile.settings
    )

  def profileListToResponse(profiles: List[Profile]): ProfileListResponse =
    ProfileListResponse(profiles.map(profileToResponse))

  def deleteProfileToResponse(success: Boolean): DeleteProfileResponse =
    DeleteProfileResponse(
      success = success,
      message =
        if success then "Profile deleted successfully"
        else "Failed to delete profile"
    )
