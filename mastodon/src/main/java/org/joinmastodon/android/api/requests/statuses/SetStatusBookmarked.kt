package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class SetStatusBookmarked(id: String, bookmarked: Boolean) :
  MastodonAPIRequest<Status>(
    method = HttpMethod.POST,
    path = "/statuses/$id/${if (bookmarked) "bookmark" else "unbookmark"}",
    respClass = Status::class.java
  ) {
  init {
    setRequestBody(Any())
  }
}
