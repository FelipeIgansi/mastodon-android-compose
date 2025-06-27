package org.joinmastodon.android.api.requests.catalog

import android.net.Uri
import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.catalog.CatalogInstance


private const val HTTPS = "https"
private const val DEFAULT_API = "api.joinmastodon.org"
private const val SERVERS_ENDPOINT = "/servers"
private const val LANGUAGE = "language"
private const val CATEGORY = "category"
private const val REGISTRATIONS = "registrations"
private const val ALL = "all"

class GetCatalogInstances(
  private val lang: String?,
  private val category: String?,
  private val includeClosedSignups: Boolean
) :
  MastodonAPIRequest<MutableList<CatalogInstance>>(
    method = HttpMethod.GET,
    path = null,
    respTypeToken = object : TypeToken<MutableList<CatalogInstance>>() {}
  ) {

  override fun getURL(): Uri {

    return Uri.Builder().apply {

      scheme(HTTPS)
      authority(DEFAULT_API)
      path(SERVERS_ENDPOINT)

      if (!lang.isNullOrEmpty()) appendQueryParameter(LANGUAGE, lang)
      if (!category.isNullOrEmpty()) appendQueryParameter(CATEGORY, category)
      if (includeClosedSignups) appendQueryParameter(REGISTRATIONS, ALL)

    }.build()

  }
}