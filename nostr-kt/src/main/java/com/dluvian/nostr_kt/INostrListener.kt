package com.dluvian.nostr_kt

import rust.nostr.protocol.Event
import rust.nostr.protocol.EventId

interface INostrListener {
    fun onOpen(relayUrl: RelayUrl, msg: String)
    fun onEvent(
        subId: SubId,
        event: Event,
        relayUrl: RelayUrl?
    )

    fun onError(relayUrl: RelayUrl, msg: String, throwable: Throwable? = null)
    fun onEOSE(relayUrl: RelayUrl, subId: SubId)
    fun onClosed(relayUrl: RelayUrl, subId: SubId, reason: String)
    fun onClose(relayUrl: RelayUrl, reason: String)
    fun onFailure(relayUrl: RelayUrl, msg: String?, throwable: Throwable? = null)
    fun onOk(relayUrl: RelayUrl, id: EventId, accepted: Boolean, msg: String)
    fun onAuth(relayUrl: RelayUrl, challengeString: String)
}