package com.dluvian.voyage.ui.components.row.mainEvent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dluvian.voyage.core.ComposableContent
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.OpenCrossPostCreation
import com.dluvian.voyage.core.UnbookmarkPost
import com.dluvian.voyage.core.model.MainEvent
import com.dluvian.voyage.ui.components.chip.BookmarkChip
import com.dluvian.voyage.ui.components.chip.CrossPostChip
import com.dluvian.voyage.ui.components.chip.UpvoteChip
import com.dluvian.voyage.ui.theme.spacing

@Composable
fun MainEventActions(
    mainEvent: MainEvent,
    onUpdate: OnUpdate,
    additionalStartAction: ComposableContent = {},
    additionalEndAction: ComposableContent = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        additionalStartAction()
        Spacer(modifier = Modifier.width(spacing.tiny))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (mainEvent.isBookmarked) BookmarkChip(onClick = { onUpdate(UnbookmarkPost(postId = mainEvent.id)) })
            CrossPostChip(onClick = { onUpdate(OpenCrossPostCreation(id = mainEvent.id)) })
            additionalEndAction()
            UpvoteChip(
                upvoteCount = mainEvent.upvoteCount,
                isUpvoted = mainEvent.isUpvoted,
                postId = mainEvent.id,
                authorPubkey = mainEvent.pubkey,
                onUpdate = onUpdate
            )
        }
    }
}