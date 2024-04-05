package com.dluvian.voyage.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dluvian.voyage.R
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.OpenReplyCreation
import com.dluvian.voyage.core.model.ReplyUI
import com.dluvian.voyage.ui.theme.ReplyIcon
import com.dluvian.voyage.ui.theme.spacing

@Composable
fun ReplyRow(
    reply: ReplyUI,
    isCollapsed: Boolean,
    showDetailedReply: Boolean,
    onUpdate: OnUpdate
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.screenEdge)
    ) {
        PostRowHeader(
            trustType = reply.trustType,
            authorName = reply.authorName,
            pubkey = reply.pubkey,
            isDetailed = true,
            createdAt = reply.createdAt,
            myTopic = null,
            collapsedText = if (isCollapsed) reply.content else null,
            onUpdate = onUpdate
        )
        AnimatedVisibility(visible = !isCollapsed) {
            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = reply.content,
                maxLines = Int.MAX_VALUE,
            )
            Spacer(modifier = Modifier.height(spacing.large))
        }
        if (!isCollapsed) PostRowActions(
            id = reply.id,
            pubkey = reply.pubkey,
            myVote = reply.myVote,
            tally = reply.tally,
            onUpdate = onUpdate,
            additionalAction = {
                TextButton(
                    modifier = Modifier.height(ButtonDefaults.MinHeight),
                    onClick = { onUpdate(OpenReplyCreation(parent = reply)) }) {
                    Icon(
                        imageVector = ReplyIcon,
                        contentDescription = stringResource(id = R.string.reply)
                    )
                    if (showDetailedReply) Text(text = stringResource(id = R.string.reply))
                }
            }
        )
    }
}