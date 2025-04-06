package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Account
import org.joinmastodon.android.model.Preferences
import org.joinmastodon.android.model.StatusPrivacy

class UpdateAccountCredentialsPreferences(
  preferences: Preferences,
  locked: Boolean?,
  discoverable: Boolean?,
  indexable: Boolean?
) : MastodonAPIRequest<Account>(
  HttpMethod.PATCH,
  "/accounts/update_credentials",
  Account::class.java
) {
  init {
    val requestSource = RequestSource(
      preferences.postingDefaultVisibility,
      preferences.postingDefaultLanguage
    )

    val requestBody = Request(locked, discoverable, indexable, requestSource)

    setRequestBody(requestBody)
  }

  private data class Request(
    val locked: Boolean?,
    val discoverable: Boolean?,
    val indexable: Boolean?,
    val source: RequestSource
  )

  private data class RequestSource(
    val privacy: StatusPrivacy,
    val language: String
  )
}
