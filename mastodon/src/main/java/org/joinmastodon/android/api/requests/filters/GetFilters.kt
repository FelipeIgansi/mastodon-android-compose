package org.joinmastodon.android.api.requests.filters

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Filter

class GetFilters : MastodonAPIRequest<MutableList<Filter?>?>(
  method = HttpMethod.GET,
  path = "/filters",
  respTypeToken = object : TypeToken<MutableList<Filter?>?>() {}) {
  override fun getPathPrefix(): String {
    return "/api/v2"
  }
}
