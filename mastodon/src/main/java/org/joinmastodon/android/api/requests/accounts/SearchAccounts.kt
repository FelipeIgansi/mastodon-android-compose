package org.joinmastodon.android.api.requests.accounts

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Account

class SearchAccounts(
  q: String,
  limit: Int,
  offset: Int,
  resolve: Boolean,
  following: Boolean
) :
  MastodonAPIRequest<MutableList<Account>>(
    HttpMethod.GET,
    "/accounts/search",
    object : TypeToken<MutableList<Account>>() {}
  ) {

  init {
    addQueryParameter("q", q)
    if (limit > 0) addQueryParameter("limit", "$limit")
    if (offset > 0) addQueryParameter("offset", "$offset")
    if (resolve) addQueryParameter("resolve", "true")
    if (following) addQueryParameter("following", "true")
  }
}
