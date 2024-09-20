package com.dluvian.voyage.data.interactor

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.dluvian.voyage.core.EventIdHex
import com.dluvian.voyage.data.model.PostDetails
import com.dluvian.voyage.data.room.dao.HashtagDao
import com.dluvian.voyage.data.room.dao.MainEventDao

class PostDetailInspector(
    private val mainEventDao: MainEventDao,
    private val hashtagDao: HashtagDao,
) {
    val currentDetails: MutableState<PostDetails?> = mutableStateOf(null)

    suspend fun setPostDetails(postId: EventIdHex) {
        if (currentDetails.value?.base?.id == postId) return

        currentDetails.value = mainEventDao.getPostDetails(id = postId)?.let { base ->
            PostDetails(
                indexedTopics = hashtagDao.getHashtags(postId = postId),
                base = base,
            )
        }
    }

    fun closePostDetails() {
        currentDetails.value = null
    }
}
