package org.joinmastodon.android.api.requests.lists

import okhttp3.FormBody
import org.joinmastodon.android.api.ResultlessMastodonAPIRequest
import java.nio.charset.StandardCharsets

class AddAccountsToList(listID: String?, accountIDs: MutableCollection<String>) :
  ResultlessMastodonAPIRequest(
    method = HttpMethod.POST,
    path = "/lists/$listID/accounts"
  ) {
  init {
    val builder = FormBody.Builder(charset = StandardCharsets.UTF_8)
    for (id in accountIDs) {
      builder.add(name = "account_ids[]", value = id)
    }
    setRequestBody(builder.build())
  }
}
