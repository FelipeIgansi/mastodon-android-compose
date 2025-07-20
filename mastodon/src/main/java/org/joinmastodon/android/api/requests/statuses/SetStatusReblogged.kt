package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class SetStatusReblogged(id: String, reblogged: Boolean) :
  MastodonAPIRequest<Status>(
    method = HttpMethod.POST,
    path = "/statuses/$id/${if (reblogged) "reblog" else "unreblog"}",
    respClass = Status::class.java
  ) {
  init {
    setRequestBody(Any())
  }
}
