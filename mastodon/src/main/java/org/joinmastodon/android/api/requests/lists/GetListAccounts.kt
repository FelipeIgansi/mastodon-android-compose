package org.joinmastodon.android.api.requests.lists

import android.text.TextUtils
import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.requests.HeaderPaginationRequest
import org.joinmastodon.android.model.Account
import org.joinmastodon.android.model.HeaderPaginationList

class GetListAccounts(listID: String?, maxID: String, limit: Int) :
  HeaderPaginationRequest<Account?>(
    HttpMethod.GET,
    "/lists/$listID/accounts",
    object : TypeToken<HeaderPaginationList<Account?>?>() {}) {
  init {
    if (!TextUtils.isEmpty(maxID)) addQueryParameter("max_id", maxID)
    addQueryParameter("limit", limit.toString())
  }
}
