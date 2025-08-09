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
  inviteCode: String,
  dateOfBirth: String?
) :
  MastodonAPIRequest<Token>(
    method = HttpMethod.POST,
    path = "/accounts",
    respClass = Token::class.java
  ) {

  init {
    setRequestBody(
      Body(username, email, password, locale, reason, timezone, inviteCode, dateOfBirth)
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
    val dateOfBirth: String?,
    private val agreement: Boolean = true,
  )
}
