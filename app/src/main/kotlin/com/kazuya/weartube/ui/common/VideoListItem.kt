package com.kazuya.weartube.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.AsyncImage
import com.kazuya.weartube.R
import com.kazuya.weartube.data.VideoItem
import com.kazuya.weartube.ui.WearTubeColors

/**
 * 一覧の 1 行。タップで再生、長押しでお気に入りの登録／解除。
 * サムネイル + タイトル 2 行 + チャンネル名。タップ領域は 48dp 以上を確保する。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoListItem(
    item: VideoItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(WearTubeColors.Surface)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(56.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black),
        ) {
            if (item.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp).align(Alignment.Center),
                )
            }
            if (isFavorite) {
                Icon(
                    painter = painterResource(R.drawable.ic_star),
                    contentDescription = null,
                    tint = WearTubeColors.Red,
                    modifier = Modifier.size(12.dp).align(Alignment.TopEnd).padding(1.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
            Text(
                text = item.title.ifBlank { item.videoId },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = if (item.isLive) "LIVE" else item.channelTitle
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isLive) WearTubeColors.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
