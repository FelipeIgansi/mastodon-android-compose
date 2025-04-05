package org.joinmastodon.android.api.requests.accounts

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.requests.HeaderPaginationRequest
import org.joinmastodon.android.model.Account
import org.joinmastodon.android.model.HeaderPaginationList

class GetAccountFollowers(
  id: String,
  maxID: String?,
  limit: Int
) : HeaderPaginationRequest<Account?>(
  HttpMethod.GET,
  "/accounts/$id/followers",
  object : TypeToken<HeaderPaginationList<Account>>() {}
) {

  init {
    if (maxID != null) addQueryParameter("max_id", maxID)
    if (limit > 0) addQueryParameter("limit", "$limit")
  }
}
