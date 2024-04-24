package com.dluvian.voyage.ui.components.icon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dluvian.voyage.core.model.TrustType
import com.dluvian.voyage.ui.components.indicator.TrustBadgeIndicator
import com.dluvian.voyage.ui.theme.AccountIcon

@Composable
fun AccountIconWithBadge(trustType: TrustType, description: String) {
    Box(
        Modifier
            .height(ButtonDefaults.MinHeight)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.fillMaxSize(),
            imageVector = AccountIcon,
            contentDescription = description
        )
        TrustBadgeIndicator(trustType = trustType)
    }
}