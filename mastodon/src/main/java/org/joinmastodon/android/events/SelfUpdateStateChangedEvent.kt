package org.joinmastodon.android.events

import org.joinmastodon.android.updater.GithubSelfUpdater.UpdateState

class SelfUpdateStateChangedEvent(@JvmField val state: UpdateState?)
