package org.joinmastodon.android.api.requests.statuses

import android.net.Uri
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.Response
import org.joinmastodon.android.api.ContentUriRequestBody
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.api.ProgressListener
import org.joinmastodon.android.api.ResizedImageRequestBody
import org.joinmastodon.android.model.Attachment
import org.joinmastodon.android.ui.utils.UiUtils
import java.io.IOException

class UploadAttachment(private val uri: Uri) :
  MastodonAPIRequest<Attachment>(
    method = HttpMethod.POST,
    path = "/media",
    respClass = Attachment::class.java
  ) {
  private var progressListener: ProgressListener? = null
  private var maxImageSize = 0
  private var description: String? = null

  constructor(uri: Uri, maxImageSize: Int, description: String?) : this(uri) {
    this.maxImageSize = maxImageSize
    this.description = description
  }

  fun setProgressListener(progressListener: ProgressListener?): UploadAttachment {
    this.progressListener = progressListener
    return this
  }

  override fun getPathPrefix() = "/api/v2"


  @Throws(IOException::class)
  override fun validateAndPostprocessResponse(respObj: Attachment, httpResponse: Response) {
    if (respObj.url == null) respObj.url = ""
    super.validateAndPostprocessResponse(respObj, httpResponse)
  }

  @Throws(IOException::class)
  override fun getRequestBody(): RequestBody {
    val builder = MultipartBody.Builder()
      .setType(MultipartBody.FORM)
      .addFormDataPart(
        name = "file",
        filename = UiUtils.getFileName(uri),
        body = if (maxImageSize > 0) {
          ResizedImageRequestBody(uri, maxImageSize, progressListener)
        } else ContentUriRequestBody(uri, progressListener)
      )
    if (!description.isNullOrEmpty()) builder.addFormDataPart("description", description ?: "")
    return builder.build()
  }
}
