package org.joinmastodon.android.events

class RemoveAccountPostsEvent(
	@JvmField val accountID: String?,
	@JvmField val postsByAccountID: String?,
	@JvmField val isUnfollow: Boolean
)
