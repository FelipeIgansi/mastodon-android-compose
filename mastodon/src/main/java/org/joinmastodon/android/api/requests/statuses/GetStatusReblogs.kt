package org.joinmastodon.android.api.requests.statuses

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.requests.HeaderPaginationRequest
import org.joinmastodon.android.model.Account
import org.joinmastodon.android.model.HeaderPaginationList

class GetStatusReblogs(id: String?, maxID: String?, limit: Int) : HeaderPaginationRequest<Account?>(
  HttpMethod.GET,
  "/statuses/$id/reblogged_by",
  object : TypeToken<HeaderPaginationList<Account?>?>() {}) {
  init {
    if (maxID != null) addQueryParameter("max_id", maxID)
    if (limit > 0) addQueryParameter("limit", limit.toString() + "")
  }
}
