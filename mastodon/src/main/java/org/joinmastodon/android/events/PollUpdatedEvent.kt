package org.joinmastodon.android.events

import org.joinmastodon.android.model.Poll

class PollUpdatedEvent(@JvmField var accountID: String?, @JvmField var poll: Poll?)
