package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest

class GetDomainBlockPreview(domain: String) : MastodonAPIRequest<GetDomainBlockPreview.Response>(
  HttpMethod.GET,
  "/domain_blocks/preview",
  Response::class.java
) {
  init {
    addQueryParameter("domain", domain)
  }

  class Response {
    @JvmField
    var followingCount: Int = 0
    @JvmField
    var followersCount: Int = 0
  }
}
