package org.joinmastodon.android.events

import org.joinmastodon.android.model.FollowList

class ListCreatedEvent(@JvmField val accountID: String?, @JvmField val list: FollowList?)
