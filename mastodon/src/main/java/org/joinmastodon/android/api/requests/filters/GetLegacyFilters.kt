package org.joinmastodon.android.api.requests.filters

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.LegacyFilter

class GetLegacyFilters : MastodonAPIRequest<MutableList<LegacyFilter?>?>(
  method = HttpMethod.GET,
  path = "/filters",
  respTypeToken = object : TypeToken<MutableList<LegacyFilter?>?>() {}
)
