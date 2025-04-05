package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Account

class GetOwnAccount : MastodonAPIRequest<Account>(
  HttpMethod.GET,
  "/accounts/verify_credentials",
  Account::class.java
)
