package org.joinmastodon.android.api.requests

import androidx.core.net.toUri
import com.google.gson.reflect.TypeToken
import okhttp3.Response
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.HeaderPaginationList
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Abstract base class for Mastodon API requests that support header-based pagination.
 *
 * This class extends [MastodonAPIRequest] to handle HTTP Link headers that contain
 * pagination information. It automatically parses the Link header from API responses
 * and populates the next/previous page URIs in the response object.
 *
 * @param I The type of items contained in the paginated list
 *
 * @see MastodonAPIRequest
 * @see HeaderPaginationList
 */
abstract class HeaderPaginationRequest<I> :
  MastodonAPIRequest<HeaderPaginationList<I>> {

  constructor(
    method: HttpMethod,
    path: String,
    respClass: Class<HeaderPaginationList<I>>
  ) : super(method, path, respClass)

  constructor(
    method: HttpMethod,
    path: String,
    respTypeToken: TypeToken<HeaderPaginationList<I>>
  ) : super(method, path, respTypeToken)

  @Throws(IOException::class)
  override fun validateAndPostprocessResponse(
    respObj: HeaderPaginationList<I>,
    httpResponse: Response
  ) {
    super.validateAndPostprocessResponse(respObj, httpResponse)

    val link = httpResponse.header("Link") ?: return

    parseLinkHeader(link, respObj)
  }

  private fun parseLinkHeader(
    link: String,
    respObj: HeaderPaginationList<I>
  ) {
    val matcher: Matcher = LINK_HEADER_PATTERN.matcher(link)
    var currentUrl: String? = null

    while (matcher.find()) {

      if (currentUrl == null) currentUrl = matcher.group(1) ?: continue

      else {

        val paramName = matcher.group(2) ?: return
        val paramValue = matcher.group(3) ?: return

        if (paramName == "rel") {

          when (paramValue) {

            "next" -> respObj.nextPageUri = currentUrl.toUri()
            "prev" -> respObj.prevPageUri = currentUrl.toUri()

          }

          currentUrl = null
        }
      }
    }
  }

  companion object {
    private val LINK_HEADER_PATTERN: Pattern =
      Pattern.compile("(?:(?:,\\s*)?<([^>]+)>|;\\s*(\\w+)=['\"](\\w+)['\"])")
  }
}
