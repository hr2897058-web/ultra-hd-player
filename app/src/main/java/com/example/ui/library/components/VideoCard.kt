package com.example.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.media.ResolutionCategory
import com.example.media.VideoItem
import com.example.ui.theme.Badge16K
import com.example.ui.theme.Badge8K
import com.example.ui.theme.BadgeHdr
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.Formatters

@Composable
fun VideoCard(
    video: VideoItem,
    isGridView: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isGridView) {
        VideoGridCard(
            video = video,
            onClick = onClick,
            onFavoriteToggle = onFavoriteToggle,
            onInfoClick = onInfoClick,
            modifier = modifier
        )
    } else {
        VideoListCard(
            video = video,
            onClick = onClick,
            onFavoriteToggle = onFavoriteToggle,
            onInfoClick = onInfoClick,
            modifier = modifier
        )
    }
}

@Composable
fun VideoGridCard(
    video: VideoItem,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("video_grid_card_${video.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(DarkSurfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.uri)
                        .videoFrameMillis(2000L)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Overlay gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000)),
                                startY = 50f
                            )
                        )
                )

                // Top Resolution Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ResolutionBadgeChip(video.resolutionCategory)

                    if (video.hdrType != null) {
                        Surface(
                            color = BadgeHdr.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "HDR",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Duration badge bottom right
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = Formatters.formatDuration(video.durationMs),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                // Codec bottom left
                Surface(
                    color = Color(0xCC1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = video.codec,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )
                }

                // Playback progress indicator
                if (video.isPartiallyPlayed) {
                    LinearProgressIndicator(
                        progress = { video.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = CyanNeon,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            // Info section
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = video.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${Formatters.formatFileSize(video.sizeBytes)} • ${video.resolutionString}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )

                    Row {
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (video.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Favorite",
                                tint = if (video.isFavorite) CyanNeon else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onInfoClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Video Details",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoListCard(
    video: VideoItem,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("video_list_card_${video.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.uri)
                        .videoFrameMillis(2000L)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Resolution badge
                Box(modifier = Modifier.padding(4.dp).align(Alignment.TopStart)) {
                    ResolutionBadgeChip(video.resolutionCategory)
                }

                // Duration badge
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = Formatters.formatDuration(video.durationMs),
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                if (video.isPartiallyPlayed) {
                    LinearProgressIndicator(
                        progress = { video.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .align(Alignment.BottomCenter),
                        color = CyanNeon,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = video.resolutionString,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = video.resolutionCategory.badgeColor
                    )
                    Text(
                        text = " • ${video.codec}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    if (video.hdrType != null) {
                        Text(
                            text = " • HDR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BadgeHdr
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${Formatters.formatFileSize(video.sizeBytes)} • ${video.folderName}",
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action buttons
            Row {
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (video.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Favorite",
                        tint = if (video.isFavorite) CyanNeon else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Video Details",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ResolutionBadgeChip(category: ResolutionCategory) {
    val (bgGradient, textColor) = when (category) {
        ResolutionCategory.ULTRA_16K -> listOf(Color(0xFFE11D48), Color(0xFFF43F5E)) to Color.White
        ResolutionCategory.UHD_8K -> listOf(Color(0xFF7C3AED), Color(0xFFA855F7)) to Color.White
        ResolutionCategory.UHD_4K -> listOf(Color(0xFF0284C7), Color(0xFF00E5FF)) to Color.Black
        ResolutionCategory.QHD_1440P -> listOf(Color(0xFF0369A1), Color(0xFF38BDF8)) to Color.White
        ResolutionCategory.FHD_1080P -> listOf(Color(0xFF059669), Color(0xFF10B981)) to Color.White
        ResolutionCategory.HD_720P -> listOf(Color(0xFF475569), Color(0xFF64748B)) to Color.White
        else -> listOf(Color(0xFF334155), Color(0xFF475569)) to Color.White
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        modifier = Modifier.background(
            Brush.horizontalGradient(bgGradient),
            RoundedCornerShape(4.dp)
        )
    ) {
        Text(
            text = category.label,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}
