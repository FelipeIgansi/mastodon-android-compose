package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Relationship

class SetAccountMuted(
  id: String,
  muted: Boolean
) :
  MastodonAPIRequest<Relationship>(
    HttpMethod.POST,
    "/accounts/$id/${if (muted) "mute" else "unmute"}",
    Relationship::class.java
  ) {
  init {
    setRequestBody(Any())
  }
}
