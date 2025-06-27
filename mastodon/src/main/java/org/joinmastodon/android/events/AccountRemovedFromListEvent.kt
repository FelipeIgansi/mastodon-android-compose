package org.joinmastodon.android.events

class AccountRemovedFromListEvent(
  @JvmField val accountID: String?,
  @JvmField val listID: String?,
  @JvmField val targetAccountID: String?
)
