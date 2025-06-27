package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Preferences

class GetPreferences : MastodonAPIRequest<Preferences>(
  method = HttpMethod.GET,
  path = "/preferences",
  respClass = Preferences::class.java
)
