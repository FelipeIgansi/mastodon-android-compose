package org.joinmastodon.android.api.requests.oauth

import org.joinmastodon.android.api.MastodonAPIRequest

class RevokeOauthToken(
  clientID: String,
  clientSecret: String,
  token: String
) : MastodonAPIRequest<Any>(
  method = HttpMethod.POST,
  path = "/oauth/revoke",
  respClass = Any::class.java
) {
  init {
    setRequestBody(Body(clientID, clientSecret, token))
  }

  override fun getPathPrefix() = ""

  private data class Body(
    val clientId: String,
    val clientSecret: String,
    val token: String
  )
}
