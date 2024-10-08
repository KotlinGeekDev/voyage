package com.dluvian.voyage.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.ThumbUpAlt
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.dluvian.voyage.core.model.FriendTrust
import com.dluvian.voyage.core.model.IsInListTrust
import com.dluvian.voyage.core.model.Locked
import com.dluvian.voyage.core.model.LockedOneself
import com.dluvian.voyage.core.model.Muted
import com.dluvian.voyage.core.model.NoTrust
import com.dluvian.voyage.core.model.Oneself
import com.dluvian.voyage.core.model.TrustType
import com.dluvian.voyage.core.model.WebTrust

val MenuIcon = Icons.Default.Menu
val HomeIcon = Icons.Default.Home
val DiscoverIcon = Icons.Default.TravelExplore
val AddIcon = Icons.Default.Add
val RemoveCircleIcon = Icons.Default.RemoveCircle
val InboxIcon = Icons.Default.Notifications
val SettingsIcon = Icons.Default.Settings
val BackIcon = Icons.AutoMirrored.Filled.ArrowBack
val CommentIcon = Icons.AutoMirrored.Filled.Comment
val AccountIcon = Icons.Default.AccountCircle
val HashtagIcon = Icons.Default.Tag
val UpvoteIcon = Icons.Default.ThumbUpAlt
val UpvoteOffIcon = Icons.Default.ThumbUpOffAlt
val SearchIcon = Icons.Default.Search
val SendIcon = Icons.AutoMirrored.Filled.Send
val ReplyIcon = Icons.AutoMirrored.Filled.Reply
val ScrollUpIcon = Icons.Default.KeyboardDoubleArrowUp
val SaveIcon = Icons.Default.Save
val ExpandIcon = Icons.Default.ExpandMore
val CollapseIcon = Icons.Default.ExpandLess
val HorizMoreIcon = Icons.Default.MoreHoriz
val DeleteIcon = Icons.Default.Delete
val RemoveIcon = Icons.Default.Close
val CrossPostIcon = Icons.Default.Repeat
val KeyIcon = Icons.Default.Key
val LightningIcon = Icons.Default.OfflineBolt
val ListIcon = Icons.AutoMirrored.Filled.List
val ViewListIcon = Icons.AutoMirrored.Filled.ViewList
val BookmarksIcon = Icons.Default.Bookmarks
val BookmarkIcon = Icons.Filled.Bookmark
val RelayIcon = Icons.Default.CellTower
val OpenIcon = Icons.AutoMirrored.Filled.OpenInNew
val EditIcon = Icons.Default.Edit
val MuteIcon = Icons.AutoMirrored.Filled.VolumeOff
val WordIcon = Icons.Default.Abc
val FilterIcon = Icons.Default.Tune
val WarningIcon = Icons.Default.WarningAmber


@Stable
@Composable
fun getTrustIcon(trustType: TrustType): ImageVector {
    return when (trustType) {
        Oneself, LockedOneself -> Icons.Default.Star
        FriendTrust, WebTrust -> Icons.Filled.VerifiedUser
        IsInListTrust -> ListIcon
        NoTrust -> Icons.Default.QuestionMark
        Muted -> MuteIcon
        Locked -> WarningIcon
    }
}

@Stable
@Composable
fun getAccountIcon(isLocked: Boolean): ImageVector {
    return if (isLocked) getTrustIcon(trustType = Locked) else AccountIcon
}
