package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Account

class GetAccountByID(id: String) : MastodonAPIRequest<Account>(
  HttpMethod.GET,
  "/accounts/$id",
  Account::class.java
)
