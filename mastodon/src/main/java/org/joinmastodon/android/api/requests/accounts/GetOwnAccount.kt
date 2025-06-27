package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Account

class GetOwnAccount : MastodonAPIRequest<Account>(
  method = HttpMethod.GET,
  path = "/accounts/verify_credentials",
  respClass = Account::class.java
)
