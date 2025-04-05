package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Token

class RegisterAccount(
  username: String,
  email: String,
  password: String,
  locale: String,
  reason: String,
  timezone: String,
  inviteCode: String
) :
  MastodonAPIRequest<Token>(
    HttpMethod.POST,
    "/accounts",
    Token::class.java
  ) {

  init {
    setRequestBody(
      Body(username, email, password, locale, reason, timezone, inviteCode)
    )
  }

  private data class Body(
    val username: String,
    val email: String,
    val password: String,
    val locale: String,
    val reason: String,
    val timeZone: String,
    val inviteCode: String,
    val agreement: Boolean = true // valor padrão mantido
  )
}
