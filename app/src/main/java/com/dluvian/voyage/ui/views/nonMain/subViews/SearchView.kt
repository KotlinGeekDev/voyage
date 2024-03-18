package com.dluvian.voyage.ui.views.nonMain.subViews

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.dluvian.voyage.R
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.OpenProfile
import com.dluvian.voyage.core.OpenTopic
import com.dluvian.voyage.core.viewModel.SearchViewModel
import com.dluvian.voyage.ui.components.ClickableRow
import com.dluvian.voyage.ui.theme.HashtagIcon
import com.dluvian.voyage.ui.theme.spacing

@Composable
fun SearchView(vm: SearchViewModel, onUpdate: OnUpdate) {
    val topics by vm.topics
    val profiles by vm.profiles

    LaunchedEffect(key1 = Unit) {
        vm.subProfiles()
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Spacer(modifier = Modifier.height(spacing.large)) }
        if (topics.isNotEmpty()) {
            item {
                SectionHeader(header = stringResource(id = R.string.topics))
            }
        }
        items(topics) { topic ->
            ClickableRow(
                header = topic,
                imageVector = HashtagIcon,
                onClick = { onUpdate(OpenTopic(topic = "#$topic")) })
        }
        if (profiles.isNotEmpty()) {
            item {
                SectionHeader(header = stringResource(id = R.string.profiles))
            }
        }
        items(profiles) { profile ->
            ClickableRow(
                header = profile.name ?: profile.pubkey,
                onClick = { onUpdate(OpenProfile(nip19 = profile.toNip19())) })
        }
    }
}

@Composable
private fun SectionHeader(header: String) {
    Text(
        modifier = Modifier.padding(
            horizontal = spacing.bigScreenEdge,
            vertical = spacing.large
        ),
        text = header,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold
    )
}
