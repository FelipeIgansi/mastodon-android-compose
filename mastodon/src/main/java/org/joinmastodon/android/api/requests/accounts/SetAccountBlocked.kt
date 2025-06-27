package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Relationship

class SetAccountBlocked(
  id: String,
  blocked: Boolean
) :
  MastodonAPIRequest<Relationship>(
    method = HttpMethod.POST,
    path = "/accounts/$id/${if (blocked) "block" else "unblock"}",
    respClass = Relationship::class.java
  ) {
  init {
    setRequestBody(Any())
  }
}
