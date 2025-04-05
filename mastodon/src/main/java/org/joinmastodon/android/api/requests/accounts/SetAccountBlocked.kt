package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Relationship

class SetAccountBlocked(
  id: String,
  blocked: Boolean
) :
  MastodonAPIRequest<Relationship>(
    HttpMethod.POST,
    "/accounts/$id/${ if (blocked) "block" else "unblock" }",
    Relationship::class.java
  ) {
  init {
    setRequestBody(Any())
  }
}
