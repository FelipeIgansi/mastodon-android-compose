package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Relationship

class SetAccountFollowed(
  id: String,
  followed: Boolean,
  showReblogs: Boolean,
  notify: Boolean
) :
  MastodonAPIRequest<Relationship>(
    HttpMethod.POST,
    "/accounts/$id/${ if (followed) "follow" else "unfollow" }",
    Relationship::class.java
  ) {
  init {
    if (followed) setRequestBody(Request(showReblogs, notify))
    else setRequestBody(Any())
  }

  private data class Request(val reblogs: Boolean, val notify: Boolean)
}
