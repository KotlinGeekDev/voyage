package com.dluvian.voyage.core.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dluvian.voyage.core.HomeViewAction
import com.dluvian.voyage.core.HomeViewAppend
import com.dluvian.voyage.core.HomeViewRefresh
import com.dluvian.voyage.core.model.Paginator
import com.dluvian.voyage.data.model.HomeFeedSetting
import com.dluvian.voyage.data.nostr.NostrSubscriber
import com.dluvian.voyage.data.provider.FeedProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class HomeViewModel(
    feedProvider: FeedProvider,
    private val nostrSubscriber: NostrSubscriber
) : ViewModel() {
    val paginator = Paginator(feedProvider = feedProvider, scope = viewModelScope)

    init {
        paginator.init(setting = HomeFeedSetting)
    }

    fun handle(homeViewAction: HomeViewAction) {
        when (homeViewAction) {
            is HomeViewRefresh -> refresh()
            is HomeViewAppend -> paginator.append()
        }
    }

    private var job: Job? = null

    fun subMyAccountAndTrustData() {
        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            nostrSubscriber.subMyAccountAndTrustData()
        }
    }

    private fun refresh() {
        paginator.refresh(onSub = { subMyAccountAndTrustData() })
    }
}
