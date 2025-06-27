package org.joinmastodon.android.api.requests.accounts

import android.text.TextUtils
import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class GetAccountStatuses(
  id: String,
  maxID: String?,
  minID: String?,
  limit: Int,
  filter: Filter,
  hashtag: String?
) : MastodonAPIRequest<MutableList<Status>>(
  method = HttpMethod.GET,
  path = "/accounts/$id/statuses",
  respTypeToken = object : TypeToken<MutableList<Status>>() {}
) {

  init {
    maxID?.let { addQueryParameter("max_id", it) }
    minID?.let { addQueryParameter("min_id", it) }

    if (limit > 0) addQueryParameter("limit", "$limit")

    when (filter) {
      Filter.DEFAULT -> addQueryParameter("exclude_replies", "true")
      Filter.INCLUDE_REPLIES -> {}
      Filter.MEDIA -> addQueryParameter("only_media", "true")
      Filter.NO_REBLOGS -> {
        addQueryParameter("exclude_replies", "true")
        addQueryParameter("exclude_reblogs", "true")
      }

      Filter.OWN_POSTS_AND_REPLIES -> addQueryParameter("exclude_reblogs", "true")
      Filter.PINNED -> addQueryParameter("pinned", "true")
    }
    if (!TextUtils.isEmpty(hashtag)) hashtag?.let { addQueryParameter("tagged", it) }
  }

  enum class Filter {
    DEFAULT,
    INCLUDE_REPLIES,
    MEDIA,
    NO_REBLOGS,
    OWN_POSTS_AND_REPLIES,
    PINNED
  }
}
