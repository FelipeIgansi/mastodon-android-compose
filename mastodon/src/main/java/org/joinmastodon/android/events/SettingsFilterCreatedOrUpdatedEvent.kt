package org.joinmastodon.android.events

import org.joinmastodon.android.model.Filter

class SettingsFilterCreatedOrUpdatedEvent(
  @JvmField val accountID: String?,
  @JvmField val filter: Filter?
)
