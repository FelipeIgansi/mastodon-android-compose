package org.joinmastodon.android.events

import org.joinmastodon.android.model.FollowList

class ListUpdatedEvent(@JvmField val accountID: String?, @JvmField val list: FollowList?)
