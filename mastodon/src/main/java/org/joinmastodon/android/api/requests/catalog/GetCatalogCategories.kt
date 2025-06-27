package org.joinmastodon.android.api.requests.catalog

import android.net.Uri
import android.text.TextUtils
import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.catalog.CatalogCategory

class GetCatalogCategories(private val lang: String?) :
  MastodonAPIRequest<MutableList<CatalogCategory>>(
    method = HttpMethod.GET,
    path = null,
    respTypeToken = object : TypeToken<MutableList<CatalogCategory>>() {}
  ) {
  override fun getURL(): Uri {

    return Uri.Builder().apply {

      scheme("https")
      authority("api.joinmastodon.org")
      path("/categories")

      if (!TextUtils.isEmpty(lang)) {
        appendQueryParameter("language", lang)
      }
    }.build()

  }
}
