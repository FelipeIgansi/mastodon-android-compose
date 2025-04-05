package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Preferences

class GetPreferences : MastodonAPIRequest<Preferences>(
  HttpMethod.GET,
  "/preferences",
  Preferences::class.java
)
