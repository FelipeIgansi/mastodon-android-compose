package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Relationship

class AcceptFollowRequest(accountID: String) :
  MastodonAPIRequest<Relationship>(
    method = HttpMethod.POST,
    path = "/follow_requests/$accountID/authorize",
    respClass = Relationship::class.java
  ) {
  init {
    setRequestBody(Any())
  }
}
