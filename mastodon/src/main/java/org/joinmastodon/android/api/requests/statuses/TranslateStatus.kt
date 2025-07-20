package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Translation

class TranslateStatus(id: String, lang: String) :
  MastodonAPIRequest<Translation>(
    method = HttpMethod.POST,
    path = "/statuses/$id/translate",
    respClass = Translation::class.java
  ) {
  init {
    setRequestBody(mapOf("lang" to lang))
  }
}
