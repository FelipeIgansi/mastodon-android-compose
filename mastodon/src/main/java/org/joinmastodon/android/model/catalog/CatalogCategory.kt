package org.joinmastodon.android.model.catalog

import org.joinmastodon.android.api.AllFieldsAreRequired
import org.joinmastodon.android.model.BaseModel

@AllFieldsAreRequired
class CatalogCategory : BaseModel() {
  var category: String? = null
  var serversCount: Int = 0

  override fun toString() = "CatalogCategory{category='$category', serversCount=$serversCount}"
}
