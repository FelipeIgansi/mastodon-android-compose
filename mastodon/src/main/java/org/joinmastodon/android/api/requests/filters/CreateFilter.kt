package org.joinmastodon.android.api.requests.filters

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Filter
import org.joinmastodon.android.model.FilterAction
import org.joinmastodon.android.model.FilterContext
import org.joinmastodon.android.model.FilterKeyword
import java.util.EnumSet
import java.util.stream.Collectors

class CreateFilter(
  title: String,
  context: EnumSet<FilterContext>,
  action: FilterAction,
  expiresIn: Int,
  words: MutableList<FilterKeyword>
) : MastodonAPIRequest<Filter>(
  method = HttpMethod.POST,
  path = "/filters",
  respClass = Filter::class.java
) {
  init {

    val keywordsAttributes = words.stream()
      .map { word: FilterKeyword ->
        KeywordAttribute(
          id = null,
          delete = null,
          keyword = word.keyword,
          wholeWord = word.wholeWord
        )
      }.collect(Collectors.toList())

    val body = FilterRequest(
      title = title,
      context = context,
      filterAction = action,
      expiresIn = if (expiresIn == 0) null else expiresIn,
      keywordsAttributes = keywordsAttributes
    )
    setRequestBody(body)
  }

  override fun getPathPrefix() = "/api/v2"
}
