package com.dluvian.voyage.ui.views.nonMain.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dluvian.voyage.R
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.ThreadViewRefresh
import com.dluvian.voyage.core.model.CommentUI
import com.dluvian.voyage.core.model.RootPostUI
import com.dluvian.voyage.core.model.ThreadUI
import com.dluvian.voyage.core.viewModel.ThreadViewModel
import com.dluvian.voyage.ui.components.BaseHint
import com.dluvian.voyage.ui.components.PullRefreshBox
import com.dluvian.voyage.ui.theme.sizing
import com.dluvian.voyage.ui.theme.superLight

@Composable
fun ThreadView(vm: ThreadViewModel, snackbar: SnackbarHostState, onUpdate: OnUpdate) {
    val isRefreshing by vm.isRefreshing
    val thread by vm.thread.collectAsState()

    ThreadScaffold(snackbar = snackbar, onUpdate = onUpdate) {
        thread?.let {
            ThreadViewContent(
                thread = it,
                isRefreshing = isRefreshing,
                onUpdate = onUpdate
            )
        }
    }
}

@Composable
private fun ThreadViewContent(
    thread: ThreadUI,
    isRefreshing: Boolean,
    onUpdate: OnUpdate
) {
    PullRefreshBox(isRefreshing = isRefreshing, onRefresh = { onUpdate(ThreadViewRefresh) }) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (thread.rootPost != null) {
                item { RootPost(rootPost = thread.rootPost) }
            }
            items(thread.comments) { comment ->
                CommentDivider()
                Comment(comment = comment)
            }
            if (thread.comments.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillParentMaxHeight(0.5f)) {
                        BaseHint(text = stringResource(id = R.string.no_comments_found))
                    }
                }
            }
        }
    }
}

@Composable
private fun RootPost(rootPost: RootPostUI) {
    Text(text = rootPost.content)
}

@Composable
private fun Comment(comment: CommentUI) {
    Text(text = comment.content)
}

@Composable
private fun CommentDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .height(sizing.bigDivider)
            .background(color = MaterialTheme.colorScheme.onBackground.superLight())
    )
}
