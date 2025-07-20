package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class SetStatusConversationMuted(id: String, muted: Boolean) :
  MastodonAPIRequest<Status>(
    method = HttpMethod.POST,
    path = "/statuses/$id${if (muted) "/mute" else "/unmute"}",
    respClass = Status::class.java
  ) {
  init {
    setRequestBody(Any())
  }
}
