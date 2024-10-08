package com.dluvian.voyage.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.dluvian.voyage.core.EventIdHex
import com.dluvian.voyage.data.room.view.LegacyReplyView
import com.dluvian.voyage.data.room.view.RootPostView
import kotlinx.coroutines.flow.Flow


private const val REPLY_FEED_QUERY = "SELECT * " +
        "FROM LegacyReplyView " +
        "WHERE createdAt <= :until " +
        "AND id IN (SELECT eventId FROM bookmark) " +
        "ORDER BY createdAt DESC " +
        "LIMIT :size"

private const val ROOT_POST_FEED_QUERY = "SELECT * " +
        "FROM RootPostView " +
        "WHERE createdAt <= :until " +
        "AND id IN (SELECT eventId FROM bookmark) " +
        "ORDER BY createdAt DESC " +
        "LIMIT :size"

private const val BOOKMARKED_EVENTS_EXIST_QUERY = "SELECT EXISTS(SELECT * " +
        "FROM mainEvent " +
        "WHERE id IN (SELECT eventId FROM bookmark))"

@Dao
interface BookmarkDao {
    @Query("SELECT MAX(createdAt) FROM bookmark")
    suspend fun getMaxCreatedAt(): Long?

    @Query("SELECT eventId FROM bookmark")
    suspend fun getMyBookmarks(): List<EventIdHex>

    @Query("SELECT eventId FROM bookmark WHERE eventId NOT IN (SELECT id FROM mainEvent)")
    suspend fun getUnknownBookmarks(): List<EventIdHex>

    @Query(REPLY_FEED_QUERY)
    fun getReplyFlow(until: Long, size: Int): Flow<List<LegacyReplyView>>

    @Query(REPLY_FEED_QUERY)
    suspend fun getReplies(until: Long, size: Int): List<LegacyReplyView>

    @Query(ROOT_POST_FEED_QUERY)
    fun getRootPostsFlow(until: Long, size: Int): Flow<List<RootPostView>>

    @Query(ROOT_POST_FEED_QUERY)
    suspend fun getRootPosts(until: Long, size: Int): List<RootPostView>

    @Query(BOOKMARKED_EVENTS_EXIST_QUERY)
    fun hasBookmarkedPostsFlow(): Flow<Boolean>

    @Query(
        "SELECT createdAt " +
                "FROM mainEvent " +
                "WHERE createdAt <= :until " +
                "AND id IN (SELECT eventId FROM bookmark) " +
                "AND id NOT IN (SELECT eventId FROM crossPost) " + // No crossposts in bookmark feed
                "ORDER BY createdAt DESC " +
                "LIMIT :size"
    )
    suspend fun getBookmarkedPostsCreatedAt(until: Long, size: Int): List<Long>
}
