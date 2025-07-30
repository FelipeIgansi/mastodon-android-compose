package org.joinmastodon.android.api.requests.oauth

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.api.session.AccountSessionManager.Companion.REDIRECT_URI
import org.joinmastodon.android.api.session.AccountSessionManager.Companion.SCOPE
import org.joinmastodon.android.model.Application

class CreateOAuthApp : MastodonAPIRequest<Application>(
  method = HttpMethod.POST,
  path = "/apps",
  respClass = Application::class.java
) {
  init {
    setRequestBody(Request())
  }

  private data class Request (
    val clientName: String = "Mastodon for Android",
    val redirectUris: String = REDIRECT_URI,
    val scopes: String = SCOPE,
    val website: String = "https://app.joinmastodon.org/android"
  )
}
