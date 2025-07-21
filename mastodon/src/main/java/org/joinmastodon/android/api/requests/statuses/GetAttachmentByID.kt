package org.joinmastodon.android.api.requests.statuses

import okhttp3.Response
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Attachment
import java.io.IOException

class GetAttachmentByID(id: String) :
  MastodonAPIRequest<Attachment>(
    method = HttpMethod.GET,
    path = "/media/$id",
    respClass = Attachment::class.java
  ) {
  @Throws(IOException::class)
  override fun validateAndPostprocessResponse(
    respObj: Attachment,
    httpResponse: Response
  ) {
    if (httpResponse.code == 206) {
      respObj.url = ""
    }
    super.validateAndPostprocessResponse(
      respObj,
      httpResponse
    )
  }
}
