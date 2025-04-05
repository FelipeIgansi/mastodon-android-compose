package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest

class SetDomainBlocked(
  domain: String,
  blocked: Boolean
) :
  MastodonAPIRequest<Any>(
    if (blocked) HttpMethod.POST else HttpMethod.DELETE,
    "/domain_blocks",
    Any::class.java
  ) {
  init {
    setRequestBody(Request(domain))
  }

  private data class Request(val domain: String)
}
