package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class SetStatusPinned(id: String, pinned: Boolean) :
  MastodonAPIRequest<Status>(
    method = HttpMethod.POST,
    path = "/statuses/$id/${if (pinned) "pin" else "unpin"}",
    respClass = Status::class.java
  ) {
  init {
    setRequestBody(Any())
  }
}
