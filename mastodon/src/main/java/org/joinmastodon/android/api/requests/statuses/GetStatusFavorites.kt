package org.joinmastodon.android.api.requests.statuses

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.requests.HeaderPaginationRequest
import org.joinmastodon.android.model.Account
import org.joinmastodon.android.model.HeaderPaginationList

class GetStatusFavorites(id: String?, maxID: String?, limit: Int) :
  HeaderPaginationRequest<Account>(
    method = HttpMethod.GET,
    path = "/statuses/$id/favourited_by",
    respTypeToken = object : TypeToken<HeaderPaginationList<Account>>() {}) {
  init {
    if (maxID != null) addQueryParameter("max_id", maxID)
    if (limit > 0) addQueryParameter("limit", limit.toString() + "")
  }
}
