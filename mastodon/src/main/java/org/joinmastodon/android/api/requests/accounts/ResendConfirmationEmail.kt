package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest

class ResendConfirmationEmail(email: String?) :
  MastodonAPIRequest<Any>(
    method = HttpMethod.POST,
    path = "/emails/confirmations",
    respClass = Any::class.java
  ) {

  init {
//  setRequestBody(Body(email))
    setRequestBody(Any())
  }

  private data class Body(val email: String?)
}
