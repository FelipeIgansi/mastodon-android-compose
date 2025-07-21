package org.joinmastodon.android.model.catalog

import org.joinmastodon.android.api.AllFieldsAreRequired
import org.joinmastodon.android.model.BaseModel

@AllFieldsAreRequired
class CatalogDefaultInstance : BaseModel() {
  @JvmField var domain: String? = null
  @JvmField var weight: Float = 0f
}
