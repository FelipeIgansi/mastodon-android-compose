package org.joinmastodon.android.events

import org.joinmastodon.android.model.Account

class AccountAddedToListEvent(
  @JvmField val accountID: String?,
  @JvmField val listID: String?,
  @JvmField val account: Account?
)
