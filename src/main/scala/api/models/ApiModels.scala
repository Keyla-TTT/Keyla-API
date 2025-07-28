package api.models

import api.models.analytics.*
import api.models.common.*
import api.models.config.*
import api.models.stats.*
import api.models.typingtest.*
import api.models.users.*

object ApiModels:
  export api.models.users.UsersModels.*
  export api.models.typingtest.TypingTestModels.*
  export api.models.stats.StatsModels.*
  export api.models.analytics.AnalyticsModels.*
  export api.models.config.ConfigModels.*
  export api.models.common.CommonModels.*
  export api.models.users.UsersModels.given
  export api.models.typingtest.TypingTestModels.given
  export api.models.stats.StatsModels.given
  export api.models.analytics.AnalyticsModels.given
  export api.models.config.ConfigModels.given
