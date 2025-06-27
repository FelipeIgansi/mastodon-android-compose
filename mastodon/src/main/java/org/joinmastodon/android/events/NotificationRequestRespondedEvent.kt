package org.joinmastodon.android.events

class NotificationRequestRespondedEvent(
  @JvmField val accountID: String?,
  @JvmField val requestID: String?
)
