package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Relationship

class AcceptFollowRequest(accountID: String) :
  MastodonAPIRequest<Relationship>(
    HttpMethod.POST,
    "/follow_requests/$accountID/authorize",
    Relationship::class.java
  ) {
  init {
    setRequestBody(Any())
  }
}
