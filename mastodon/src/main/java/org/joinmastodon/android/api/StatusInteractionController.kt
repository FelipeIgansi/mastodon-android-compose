package org.joinmastodon.android.api

import android.os.Looper
import me.grishka.appkit.api.Callback
import me.grishka.appkit.api.ErrorResponse
import org.joinmastodon.android.E.post
import org.joinmastodon.android.MastodonApp
import org.joinmastodon.android.api.requests.statuses.SetStatusBookmarked
import org.joinmastodon.android.api.requests.statuses.SetStatusFavorited
import org.joinmastodon.android.api.requests.statuses.SetStatusReblogged
import org.joinmastodon.android.events.CounterType
import org.joinmastodon.android.events.StatusCountersUpdatedEvent
import org.joinmastodon.android.model.Status

class StatusInteractionController(private val accountID: String) {

  private val runningFavoriteRequests = HashMap<String, SetStatusFavorited>()
  private val runningReblogRequests = HashMap<String, SetStatusReblogged>()
  private val runningBookmarkRequests = HashMap<String, SetStatusBookmarked>()

  fun setFavorited(status: Status, favorited: Boolean) {
    check(Looper.getMainLooper().isCurrentThread) { "Can only be called from main thread" }

    runningFavoriteRequests.remove(status.id)?.cancel()

    val request = SetStatusFavorited(status.id, favorited)
      .setCallback(object : Callback<Status> {

        override fun onSuccess(result: Status) {
          runningFavoriteRequests.remove(status.id)
          post(StatusCountersUpdatedEvent(result, CounterType.FAVORITES))
        }

        override fun onError(error: ErrorResponse) {
          runningFavoriteRequests.remove(status.id)
          error.showToast(MastodonApp.context)
          status.favourited = !favorited
          if (favorited) status.favouritesCount--
          else status.favouritesCount++
          post(StatusCountersUpdatedEvent(status, CounterType.FAVORITES))
        }
      })
      .exec(accountID) as SetStatusFavorited

    runningFavoriteRequests.put(status.id, request)
    status.favourited = favorited
    if (favorited) status.favouritesCount++
    else status.favouritesCount--

    post(StatusCountersUpdatedEvent(status, CounterType.FAVORITES))
  }

  fun setReblogged(status: Status, reblogged: Boolean) {
    check(Looper.getMainLooper().isCurrentThread) { "Can only be called from main thread" }

    runningReblogRequests.remove(status.id)?.cancel()

    val request = SetStatusReblogged(status.id, reblogged)
      .setCallback(object : Callback<Status> {

        override fun onSuccess(result: Status) {
          runningReblogRequests.remove(status.id)
          post(StatusCountersUpdatedEvent(result, CounterType.REBLOGS))
        }

        override fun onError(error: ErrorResponse) {
          runningReblogRequests.remove(status.id)
          error.showToast(MastodonApp.context)
          status.reblogged = !reblogged
          if (reblogged) status.reblogsCount--
          else status.reblogsCount++
          post(StatusCountersUpdatedEvent(status, CounterType.REBLOGS))
        }
      })
      .exec(accountID) as SetStatusReblogged

    runningReblogRequests.put(status.id, request)
    status.reblogged = reblogged
    if (reblogged) status.reblogsCount++
    else status.reblogsCount--

    post(StatusCountersUpdatedEvent(status, CounterType.REBLOGS))
  }

  fun setBookmarked(status: Status, bookmarked: Boolean) {
    check(Looper.getMainLooper().isCurrentThread) { "Can only be called from main thread" }

    runningBookmarkRequests.remove(status.id)?.cancel()

    val request = SetStatusBookmarked(status.id, bookmarked)
      .setCallback(object : Callback<Status> {

        override fun onSuccess(result: Status) {
          runningBookmarkRequests.remove(status.id)
          post(StatusCountersUpdatedEvent(result, CounterType.BOOKMARKS))
        }

        override fun onError(error: ErrorResponse) {
          runningBookmarkRequests.remove(status.id)
          error.showToast(MastodonApp.context)
          status.bookmarked = !bookmarked
          post(StatusCountersUpdatedEvent(status, CounterType.BOOKMARKS))
        }
      })
      .exec(accountID) as SetStatusBookmarked

    runningBookmarkRequests.put(status.id, request)
    status.bookmarked = bookmarked

    post(StatusCountersUpdatedEvent(status, CounterType.BOOKMARKS))
  }
}