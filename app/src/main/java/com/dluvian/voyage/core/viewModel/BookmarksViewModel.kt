package com.dluvian.voyage.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dluvian.voyage.core.BookmarksViewAction
import com.dluvian.voyage.core.BookmarksViewAppend
import com.dluvian.voyage.core.BookmarksViewInit
import com.dluvian.voyage.core.BookmarksViewRefresh
import com.dluvian.voyage.core.launchIO
import com.dluvian.voyage.core.model.Paginator
import com.dluvian.voyage.data.model.BookmarksFeedSetting
import com.dluvian.voyage.data.nostr.LazyNostrSubscriber
import com.dluvian.voyage.data.provider.FeedProvider

class BookmarksViewModel(
    feedProvider: FeedProvider,
    val feedState: LazyListState,
    private val lazyNostrSubscriber: LazyNostrSubscriber,
) : ViewModel() {
    val paginator = Paginator(
        feedProvider = feedProvider,
        scope = viewModelScope,
        subCreator = lazyNostrSubscriber.subCreator
    )

    fun handle(action: BookmarksViewAction) {
        when (action) {
            is BookmarksViewInit -> paginator.init(setting = BookmarksFeedSetting)
            is BookmarksViewRefresh -> refresh()
            is BookmarksViewAppend -> paginator.append()
        }
    }

    private fun refresh() {
        viewModelScope.launchIO {
            lazyNostrSubscriber.lazySubMyAccount()
        }
        paginator.refresh()
    }
}
