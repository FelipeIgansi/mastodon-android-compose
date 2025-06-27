package org.joinmastodon.android.api.requests.lists

import okhttp3.FormBody
import org.joinmastodon.android.api.ResultlessMastodonAPIRequest
import java.nio.charset.StandardCharsets

class RemoveAccountsFromList(listID: String?, accountIDs: MutableCollection<String>) :
  ResultlessMastodonAPIRequest(
    method = HttpMethod.DELETE,
    path = "/lists/$listID/accounts"
  ) {
  init {
    val builder = FormBody.Builder(StandardCharsets.UTF_8)
    for (id in accountIDs) {
      builder.add("account_ids[]", id)
    }
    setRequestBody(builder.build())
  }
}
