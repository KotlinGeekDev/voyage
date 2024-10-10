package com.dluvian.voyage.data.provider

import androidx.compose.runtime.State
import com.dluvian.voyage.core.EventIdHex
import com.dluvian.voyage.core.PubkeyHex
import com.dluvian.voyage.core.SHORT_DEBOUNCE
import com.dluvian.voyage.core.model.CrossPost
import com.dluvian.voyage.core.model.MainEvent
import com.dluvian.voyage.core.model.SomeReply
import com.dluvian.voyage.core.utils.containsNoneIgnoreCase
import com.dluvian.voyage.core.utils.firstThenDistinctDebounce
import com.dluvian.voyage.core.utils.mergeToMainEventUIList
import com.dluvian.voyage.core.utils.mergeToSomeReplyUIList
import com.dluvian.voyage.data.event.OldestUsedEvent
import com.dluvian.voyage.data.model.BookmarksFeedSetting
import com.dluvian.voyage.data.model.FeedSetting
import com.dluvian.voyage.data.model.ForcedData
import com.dluvian.voyage.data.model.HomeFeedSetting
import com.dluvian.voyage.data.model.InboxFeedSetting
import com.dluvian.voyage.data.model.ListFeedSetting
import com.dluvian.voyage.data.model.MainFeedSetting
import com.dluvian.voyage.data.model.ProfileFeedSetting
import com.dluvian.voyage.data.model.ReplyFeedSetting
import com.dluvian.voyage.data.model.TopicFeedSetting
import com.dluvian.voyage.data.nostr.NostrSubscriber
import com.dluvian.voyage.data.room.AppDatabase
import com.dluvian.voyage.data.room.view.CrossPostView
import com.dluvian.voyage.data.room.view.RootPostView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

class FeedProvider(
    private val nostrSubscriber: NostrSubscriber,
    private val room: AppDatabase,
    private val oldestUsedEvent: OldestUsedEvent,
    private val annotatedStringProvider: AnnotatedStringProvider,
    private val forcedVotes: Flow<Map<EventIdHex, Boolean>>,
    private val forcedFollows: Flow<Map<PubkeyHex, Boolean>>,
    private val forcedBookmarks: Flow<Map<EventIdHex, Boolean>>,
    private val muteProvider: MuteProvider,
    private val showAuthorName: State<Boolean>,
) {
    private val staticFeedProvider = StaticFeedProvider(
        room = room,
        annotatedStringProvider = annotatedStringProvider
    )

    suspend fun getStaticFeed(
        until: Long,
        size: Int,
        setting: FeedSetting,
    ): List<MainEvent> {
        return staticFeedProvider.getStaticFeed(
            until = until,
            size = size,
            setting = setting
        )
    }

    suspend fun getFeedFlow(
        until: Long,
        subUntil: Long,
        size: Int,
        setting: FeedSetting,
        forceSubscription: Boolean,
    ): Flow<List<MainEvent>> {
            nostrSubscriber.subFeed(
                until = subUntil,
                limit = size,
                setting = setting,
                forceSubscription = forceSubscription
            )

        val mutedWords = muteProvider.getMutedWords()

        return when (setting) {
            is MainFeedSetting -> getMainFeedFlow(
                until = until,
                size = size,
                setting = setting
            )

            is ReplyFeedSetting -> getReplyFeedFlow(setting = setting, until = until, size = size)

            is InboxFeedSetting -> getInboxFeedFlow(setting = setting, until = until, size = size)

            BookmarksFeedSetting -> getBookmarksFeedFlow(until = until, size = size)
        }
            .firstThenDistinctDebounce(SHORT_DEBOUNCE)
            .onEach { posts ->
                oldestUsedEvent.updateOldestCreatedAt(posts.minOfOrNull { it.createdAt })
                nostrSubscriber.subVotesAndReplies(
                    parentIds = posts.filter { it.replyCount == 0 && it.upvoteCount == 0 }
                        .filter { it.content.text.containsNoneIgnoreCase(strs = mutedWords) }
                        .map { it.getRelevantId() }
                )
                if (showAuthorName.value) {
                    val pubkeys = posts.filter { it.authorName.isNullOrEmpty() }
                        .map { it.pubkey }
                        .toMutableSet()
                    val crossPostedPubkeys = posts.mapNotNull {
                        if (it is CrossPost && it.crossPostedAuthorName.isNullOrEmpty())
                            it.crossPostedPubkey
                        else null
                    }
                    pubkeys.addAll(crossPostedPubkeys)
                    nostrSubscriber.subProfiles(pubkeys = pubkeys)
                }
            }
    }

    private fun getMainFeedFlow(
        until: Long,
        size: Int,
        setting: MainFeedSetting,
    ): Flow<List<MainEvent>> {
        val rootPosts = getRootPostFlow(setting = setting, until = until, size = size)
        val crossPosts = getCrossPostFlow(setting = setting, until = until, size = size)

        return combine(
            rootPosts.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            crossPosts.firstThenDistinctDebounce(SHORT_DEBOUNCE),
            forcedVotes,
            forcedFollows,
            forcedBookmarks,
        ) { root, cross, votes, follows, bookmarks ->
            mergeToMainEventUIList(
                roots = root,
                crossPosts = cross,
                legacyReplies = emptyList(),
                comments = emptyList(),
                votes = votes,
                follows = follows,
                bookmarks = bookmarks,
                size = size,
                annotatedStringProvider = annotatedStringProvider
            )
        }
    }

    private fun getRootPostFlow(
        setting: MainFeedSetting,
        until: Long,
        size: Int,
    ): Flow<List<RootPostView>> {
        return when (setting) {
            is HomeFeedSetting -> room.homeFeedDao().getHomeRootPostFlow(
                setting = setting,
                until = until,
                size = size
            )

            is TopicFeedSetting -> room.feedDao().getTopicRootPostFlow(
                topic = setting.topic,
                until = until,
                size = size
            )

            is ProfileFeedSetting -> room.feedDao().getProfileRootPostFlow(
                pubkey = setting.nprofile.publicKey().toHex(),
                until = until,
                size = size
            )

            is ListFeedSetting -> room.feedDao().getListRootPostFlow(
                identifier = setting.identifier,
                until = until,
                size = size
            )
        }
    }

    private fun getCrossPostFlow(
        setting: MainFeedSetting,
        until: Long,
        size: Int,
    ): Flow<List<CrossPostView>> {
        return when (setting) {
            is HomeFeedSetting -> room.homeFeedDao().getHomeCrossPostFlow(
                setting = setting,
                until = until,
                size = size
            )

            is TopicFeedSetting -> room.feedDao().getTopicCrossPostFlow(
                topic = setting.topic,
                until = until,
                size = size
            )

            is ProfileFeedSetting -> room.feedDao().getProfileCrossPostFlow(
                pubkey = setting.nprofile.publicKey().toHex(),
                until = until,
                size = size
            )

            is ListFeedSetting -> room.feedDao().getListCrossPostFlow(
                identifier = setting.identifier,
                until = until,
                size = size
            )
        }
    }

    private fun getReplyFeedFlow(
        setting: ReplyFeedSetting,
        until: Long,
        size: Int,
    ): Flow<List<SomeReply>> {
        val pubkey = setting.nprofile.publicKey().toHex()

        return combine(
            room.legacyReplyDao().getProfileReplyFlow(pubkey = pubkey, until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            room.commentDao().getProfileCommentFlow(pubkey = pubkey, until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            forcedVotes,
            forcedFollows,
            forcedBookmarks,
        ) { legacyReplies, comments, votes, follows, bookmarks ->
            mergeToSomeReplyUIList(
                legacyReplies = legacyReplies,
                comments = comments,
                votes = votes,
                follows = follows,
                bookmarks = bookmarks,
                size = size,
                annotatedStringProvider = annotatedStringProvider
            )
        }
    }

    private fun getInboxFeedFlow(
        setting: InboxFeedSetting,
        until: Long,
        size: Int
    ): Flow<List<MainEvent>> {
        return combine(
            room.inboxDao()
                .getMentionRootFlow(setting = setting, until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            room.inboxDao()
                .getInboxReplyFlow(setting = setting, until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            room.inboxDao()
                .getInboxCommentFlow(setting = setting, until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            getForcedFlow()
        ) { roots, legacyReplies, comments, forced ->
            mergeToMainEventUIList(
                roots = roots,
                crossPosts = emptyList(),
                legacyReplies = legacyReplies,
                comments = comments,
                forcedData = forced,
                size = size,
                annotatedStringProvider = annotatedStringProvider
            )
        }
    }

    private fun getBookmarksFeedFlow(until: Long, size: Int): Flow<List<MainEvent>> {
        return combine(
            room.bookmarkDao()
                .getRootPostsFlow(until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            room.bookmarkDao()
                .getReplyFlow(until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            room.bookmarkDao()
                .getCommentFlow(until = until, size = size)
                .firstThenDistinctDebounce(SHORT_DEBOUNCE),
            getForcedFlow()
        ) { roots, legacyReplies, comments, forced ->
            mergeToMainEventUIList(
                roots = roots,
                crossPosts = emptyList(),
                legacyReplies = legacyReplies,
                comments = comments,
                forcedData = forced,
                size = size,
                annotatedStringProvider = annotatedStringProvider
            )
        }
    }

    fun settingHasPostsFlow(setting: FeedSetting): Flow<Boolean> {
        return when (setting) {
            is HomeFeedSetting -> room.homeFeedDao().hasHomeFeedFlow(setting = setting)
            is TopicFeedSetting -> room.feedDao().hasTopicFeedFlow(topic = setting.topic)
            is ProfileFeedSetting -> room.feedDao()
                .hasProfileFeedFlow(pubkey = setting.nprofile.publicKey().toHex())

            is ReplyFeedSetting -> room.someReplyDao()
                .hasProfileRepliesFlow(pubkey = setting.nprofile.publicKey().toHex())

            is ListFeedSetting -> room.feedDao()
                .hasListFeedFlow(identifier = setting.identifier)

            is InboxFeedSetting -> room.inboxDao().hasInboxFlow(setting = setting)

            BookmarksFeedSetting -> room.bookmarkDao().hasBookmarkedPostsFlow()

        }
    }

    private fun getForcedFlow(): Flow<ForcedData> {
        return ForcedData.combineFlows(
            votes = forcedVotes,
            follows = forcedFollows,
            bookmarks = forcedBookmarks
        )
    }
}
