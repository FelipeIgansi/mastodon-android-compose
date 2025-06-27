package org.joinmastodon.android.api.requests.notifications

import org.joinmastodon.android.api.ResultlessMastodonAPIRequest

class RespondToNotificationRequest(
  id: String?,
  allow: Boolean
) : ResultlessMastodonAPIRequest(
  method = HttpMethod.POST,
  path = "/notifications/requests/$id${if (allow) "/accept" else "/dismiss"}"
) {
  init {
    setRequestBody(Any())
  }
}
