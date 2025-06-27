package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Account

class GetAccountByID(id: String) : MastodonAPIRequest<Account>(
  method = HttpMethod.GET,
  path = "/accounts/$id",
  respClass = Account::class.java
)
