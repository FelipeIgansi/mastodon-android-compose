package org.joinmastodon.android.api.requests.filters

import androidx.annotation.Keep
import org.joinmastodon.android.model.FilterAction
import org.joinmastodon.android.model.FilterContext
import java.util.EnumSet

@Keep
internal class FilterRequest(
  val title: String,
  val context: EnumSet<FilterContext>,
  val filterAction: FilterAction,
  val expiresIn: Int?,
  val keywordsAttributes: MutableList<KeywordAttribute>
)
