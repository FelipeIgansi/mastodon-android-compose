package org.joinmastodon.android.api.requests.catalog

import androidx.core.net.toUri
import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.catalog.CatalogDefaultInstance

class GetCatalogDefaultInstances :
  MastodonAPIRequest<MutableList<CatalogDefaultInstance>>(
    method = HttpMethod.GET,
    path = null,
    respTypeToken = object : TypeToken<MutableList<CatalogDefaultInstance>>() {}
  ) {

  init {
    setTimeout(500)
  }

  override fun getURL() = "https://api.joinmastodon.org/default-servers".toUri()

}
