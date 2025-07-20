package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class SetStatusFavorited(id: String, favorited: Boolean) :
  MastodonAPIRequest<Status>(
    method = HttpMethod.POST,
    path = "/statuses/$id/${if (favorited) "favourite" else "unfavourite"}",
    respClass = Status::class.java
  ) {
  init {
    setRequestBody(Any())
  }
}
