package org.joinmastodon.android.events

class NotificationsMarkerUpdatedEvent(
  @JvmField val accountID: String?,
  val marker: String?,
  @JvmField val clearUnread: Boolean
)
