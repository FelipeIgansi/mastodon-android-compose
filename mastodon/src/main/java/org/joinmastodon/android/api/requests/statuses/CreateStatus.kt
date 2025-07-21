package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status
import org.joinmastodon.android.model.StatusPrivacy
import java.time.Instant

class CreateStatus(req: Request, uuid: String) :
  MastodonAPIRequest<Status>(
    method = HttpMethod.POST,
    path = "/statuses",
    respClass = Status::class.java
  ) {
  init {
    setRequestBody(req)
    addHeader("Idempotency-Key", uuid)
  }

  class Request {
    @JvmField var status: String = ""
    @JvmField var mediaAttributes: MutableList<MediaAttribute> = mutableListOf()
    @JvmField var mediaIds: MutableList<String> = mutableListOf()
    @JvmField var poll: Poll? = null
    @JvmField var inReplyToId: String? = null
    @JvmField var sensitive: Boolean = false
    @JvmField var spoilerText: String? = null
    @JvmField var visibility: StatusPrivacy? = null
    @JvmField var language: String? = null

    var scheduledAt: Instant? = null


    class Poll {
      @JvmField var options: ArrayList<String> = ArrayList()
      @JvmField var expiresIn: Int = 0
      @JvmField var multiple: Boolean = false

      var hideTotals: Boolean = false
    }

    class MediaAttribute(
      var id: String?,
      var description: String?,
      var focus: String?
    )
  }
}
