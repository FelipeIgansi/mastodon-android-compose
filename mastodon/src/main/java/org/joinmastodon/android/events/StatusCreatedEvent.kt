package org.joinmastodon.android.events

import org.joinmastodon.android.model.Status

class StatusCreatedEvent(@JvmField val status: Status?, @JvmField val accountID: String?)
