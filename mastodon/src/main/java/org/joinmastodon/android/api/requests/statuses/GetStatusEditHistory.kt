package org.joinmastodon.android.api.requests.statuses

import com.google.gson.reflect.TypeToken
import okhttp3.Response
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status
import org.joinmastodon.android.model.StatusPrivacy
import java.io.IOException

class GetStatusEditHistory(id: String) :
  MastodonAPIRequest<@JvmSuppressWildcards List<Status>>(
    method = HttpMethod.GET,
    path = "/statuses/$id/history",
    respTypeToken = object : TypeToken<@JvmSuppressWildcards List<Status>>() {}
  ) {
  /**
  * suppression added, as the callback was capturing a generic, and not understanding the correct value
  * due to kotlin's automatic wildcard assignment
  **/
  @Throws(IOException::class)
  override fun validateAndPostprocessResponse(
    respObj: List<Status>,
    httpResponse: Response
  ) {
    var count = 0
    respObj.forEach { status ->
      status.uri = ""
      status.id = "fakeID$count"
      status.visibility = StatusPrivacy.PUBLIC
      status.mentions = emptyList()
      status.tags = emptyList()

      status.let { poll ->
        status.poll.id = "fakeID$count"
        status.poll.emojis = emptyList()
        status.poll.ownVotes = emptyList()
      }

      count++
    }
    super.validateAndPostprocessResponse(respObj, httpResponse)
  }
}
