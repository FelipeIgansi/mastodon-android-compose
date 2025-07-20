package org.joinmastodon.android.api.requests.notifications

import org.joinmastodon.android.api.ApiUtils.enumSetToStrings
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.NotificationType
import java.util.EnumSet

class GetUnreadNotificationsCount(
  includeTypes: EnumSet<NotificationType>?,
  groupedTypes: EnumSet<NotificationType>?
) : MastodonAPIRequest<GetUnreadNotificationsCount.Response>(
  method = HttpMethod.GET,
  path = "/notifications/unread_count",
  respClass = Response::class.java
) {
  init {

    includeTypes?.let { includedSet ->

      enumSetToStrings(includedSet, NotificationType::class.java)
        .forEach { includedTypeName ->
          addQueryParameter("types[]", includedTypeName)
        }

      enumSetToStrings(EnumSet.complementOf(includedSet), NotificationType::class.java)
        .forEach { excludedTypeName ->
          addQueryParameter("exclude_types[]", excludedTypeName)
        }
    }

    groupedTypes?.let { groupSet ->

      enumSetToStrings(groupSet, NotificationType::class.java)
        .forEach { groupedName ->
          addQueryParameter("grouped_types[]", groupedName)
        }

    }
  }

  override fun getPathPrefix() = "/api/v2"


  class Response {
    @JvmField
    var count: Int = 0
  }
}
