package com.dluvian.voyage.data.event

import android.util.Log
import com.dluvian.nostr_kt.RelayUrl
import com.dluvian.nostr_kt.SubId
import com.dluvian.nostr_kt.getHashtags
import com.dluvian.nostr_kt.getMetadata
import com.dluvian.nostr_kt.getNip65s
import com.dluvian.nostr_kt.getReplyToId
import com.dluvian.nostr_kt.getTitle
import com.dluvian.nostr_kt.isContactList
import com.dluvian.nostr_kt.isNip65
import com.dluvian.nostr_kt.isProfile
import com.dluvian.nostr_kt.isReplyPost
import com.dluvian.nostr_kt.isRootPost
import com.dluvian.nostr_kt.isTopicList
import com.dluvian.nostr_kt.isValidEventId
import com.dluvian.nostr_kt.isVote
import com.dluvian.nostr_kt.removeTrailingSlashes
import com.dluvian.nostr_kt.secs
import com.dluvian.voyage.core.EventIdHex
import com.dluvian.voyage.core.MAX_TOPICS
import com.dluvian.voyage.core.MAX_TOPIC_LEN
import com.dluvian.voyage.core.isBareTopicStr
import com.dluvian.voyage.data.account.IPubkeyProvider
import com.dluvian.voyage.data.model.FilterWrapper
import rust.nostr.protocol.Event

class EventValidator(
    private val syncedFilterCache: Map<SubId, List<FilterWrapper>>,
    private val syncedIdCache: MutableSet<EventIdHex>,
    private val pubkeyProvider: IPubkeyProvider,
) {
    private val tag = "EventValidator"

    fun getValidatedEvent(
        event: Event,
        subId: SubId,
        relayUrl: RelayUrl
    ): ValidatedEvent? {
        val idHex = event.id().toHex()
        if (syncedIdCache.contains(idHex)) return null

        if (!matchesFilter(subId = subId, event = event)) {
            Log.v(tag, "Discard event not matching filter, $idHex from $relayUrl")
            return null
        }

        val validatedEvent = validate(event = event, relayUrl = relayUrl)
        syncedIdCache.add(idHex)

        if (validatedEvent == null) {
            Log.w(tag, "Discard invalid event $idHex from $relayUrl")
            return null
        }

        return validatedEvent
    }

    private fun matchesFilter(subId: SubId, event: Event): Boolean {
        val filters = syncedFilterCache.getOrDefault(subId, emptyList()).toList()
        if (filters.isEmpty()) {
            Log.w(tag, "Filter is empty")
            return false
        }

        val matches = filters.any { it.filter.matchEvent(event = event) }
        if (!matches) return false

        val replyToId = event.getReplyToId() ?: return true

        return filters.any { it.filter.matchEvent(event = event) && it.e.contains(replyToId) }
    }

    private fun validate(event: Event, relayUrl: RelayUrl): ValidatedEvent? {
        val validatedEvent = if (event.isRootPost()) {
            val topics = event.getHashtags()
                .map { it.removePrefix("#").trim().take(MAX_TOPIC_LEN).lowercase() }
                .filter { it.isBareTopicStr() }
                .distinct()
                .take(MAX_TOPICS)
            val title = event.getTitle()?.trim()
            val content = event.content().trim()
            if (title.isNullOrEmpty() && content.isEmpty()) return null
            ValidatedRootPost(
                id = event.id().toHex(),
                pubkey = event.author().toHex(),
                topics = topics,
                title = title,
                content = content,
                createdAt = event.createdAt().secs(),
                relayUrl = relayUrl,
            )
        } else if (event.isReplyPost()) {
            val replyToId = event.getReplyToId() ?: return null
            val content = event.content().trim()
            if (content.isEmpty() || replyToId == event.id()
                    .toHex() || !isValidEventId(replyToId)
            ) {
                return null
            }
            ValidatedReply(
                id = event.id().toHex(),
                pubkey = event.author().toHex(),
                parentId = replyToId,
                content = content,
                createdAt = event.createdAt().secs(),
                relayUrl = relayUrl,
            )
        } else if (event.isVote()) {
            val postId = event.eventIds().firstOrNull() ?: return null
            ValidatedVote(
                id = event.id().toHex(),
                postId = postId.toHex(),
                pubkey = event.author().toHex(),
                isPositive = event.content() != "-",
                createdAt = event.createdAt().secs()
            )
        } else if (event.isContactList()) {
            ValidatedContactList(
                pubkey = event.author().toHex(),
                friendPubkeys = event.publicKeys().map { it.toHex() }.toSet(),
                createdAt = event.createdAt().secs()
            )
        } else if (event.isTopicList()) {
            if (event.author().toHex() != pubkeyProvider.getPubkeyHex()) return null
            ValidatedTopicList(
                myPubkey = event.author().toHex(),
                topics = event.getHashtags()
                    .map { it.trim().lowercase() }
                    .filter { it.isBareTopicStr() }
                    .toSet(),
                createdAt = event.createdAt().secs()
            )
        } else if (event.isNip65()) {
            val relays = event.getNip65s()
                .map { it.copy(url = it.url.removeTrailingSlashes()) }
                .distinct()
            if (relays.isEmpty()) return null
            ValidatedNip65(
                pubkey = event.author().toHex(),
                relays = relays,
                createdAt = event.createdAt().secs()
            )
        } else if (event.isProfile()) {
            val metadata = event.getMetadata() ?: return null
            ValidatedProfile(
                id = event.id().toHex(),
                pubkey = event.author().toHex(),
                metadata = metadata,
                createdAt = event.createdAt().secs()
            )
        } else null

        if (validatedEvent == null) return null
        if (!event.verify()) return null

        return validatedEvent
    }
}
