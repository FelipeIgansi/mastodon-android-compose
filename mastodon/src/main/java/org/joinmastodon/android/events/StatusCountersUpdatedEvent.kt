package org.joinmastodon.android.events

import org.joinmastodon.android.model.Status

class StatusCountersUpdatedEvent(status: Status,  type: CounterType) {
  @JvmField
  var id: String? = status.id
  @JvmField
  var favorites: Long = status.favouritesCount
  @JvmField
  var reblogs: Long = status.reblogsCount
  @JvmField
  var replies: Long = status.repliesCount
  @JvmField
  var favorited: Boolean = status.favourited
  @JvmField
  var reblogged: Boolean = status.reblogged
  @JvmField
  var bookmarked: Boolean = status.bookmarked
  @JvmField
  var type: CounterType = type
}
enum class CounterType {
  FAVORITES,
  REBLOGS,
  REPLIES,
  BOOKMARKS
}