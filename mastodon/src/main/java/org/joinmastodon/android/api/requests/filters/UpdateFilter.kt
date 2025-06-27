package org.joinmastodon.android.api.requests.filters

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Filter
import org.joinmastodon.android.model.FilterAction
import org.joinmastodon.android.model.FilterContext
import org.joinmastodon.android.model.FilterKeyword
import java.util.EnumSet

class UpdateFilter(
  id: String,
  title: String,
  context: EnumSet<FilterContext>,
  action: FilterAction,
  expiresIn: Int,
  words: MutableList<FilterKeyword>,
  deletedWords: MutableList<String>
) : MastodonAPIRequest<Filter>(
  method = HttpMethod.PUT,
  path = "/filters/$id",
  respClass = Filter::class.java
) {
  init {

    val attrs = words.map {
      KeywordAttribute(
        id = it.id,
        delete = null,
        keyword = it.keyword,
        wholeWord = it.wholeWord
      )
    } + deletedWords.map {
      KeywordAttribute(
        id = it,
        delete = true,
        keyword = null,
        wholeWord = null
      )
    }



    setRequestBody(
      FilterRequest(
        title = title,
        context = context,
        filterAction = action,
        expiresIn = if (expiresIn == 0) null else expiresIn,
        keywordsAttributes = attrs.toMutableList()
      )
    )
  }

  override fun getPathPrefix() = "/api/v2"

}
