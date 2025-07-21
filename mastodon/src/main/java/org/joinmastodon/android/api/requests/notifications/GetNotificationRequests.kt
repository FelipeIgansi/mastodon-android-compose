package org.joinmastodon.android.api.requests.notifications

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.requests.HeaderPaginationRequest
import org.joinmastodon.android.model.HeaderPaginationList
import org.joinmastodon.android.model.NotificationRequest

class GetNotificationRequests(maxID: String?) :
  HeaderPaginationRequest<NotificationRequest>(
    method = HttpMethod.GET,
    path = "/notifications/requests",
    respTypeToken = object : TypeToken<HeaderPaginationList<NotificationRequest>>() {}) {
  init {
    if (maxID != null) addQueryParameter("max_id", maxID)
  }
}
