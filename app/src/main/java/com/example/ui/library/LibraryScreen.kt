package com.example.ui.library

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.ResolutionCategory
import com.example.media.VideoItem
import com.example.ui.library.components.FolderCard
import com.example.ui.library.components.ResolutionBadgeChip
import com.example.ui.library.components.SortBottomSheet
import com.example.ui.library.components.VideoCard
import com.example.ui.library.components.VideoDetailsModal
import com.example.ui.theme.Badge16K
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val videos by viewModel.filteredVideos.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.scanMessage) {
        uiState.scanMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearScanMessage()
        }
    }

    // Storage Access Framework File Picker Launcher
    val openVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importSingleVideoUri(it) { item ->
                onVideoClick(item)
            }
        }
    }

    // Storage Access Framework Folder Picker Launcher
    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        treeUri?.let {
            viewModel.importFolderUri(it)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen"),
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(DarkSurface)) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface,
                        titleContentColor = TextPrimary
                    ),
                    title = {
                        if (uiState.isSearchActive) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text("Search videos, codecs, resolutions...", fontSize = 13.sp, color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_text_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanNeon,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = CyanNeon
                                ),
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear Search", tint = TextSecondary)
                                        }
                                    }
                                }
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "UltraPlayer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Badge16K.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "16K ULTRA",
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Badge16K
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.setSearchActive(!uiState.isSearchActive) },
                            modifier = Modifier.testTag("search_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (uiState.isSearchActive) CyanNeon else TextPrimary
                            )
                        }

                        IconButton(
                            onClick = { openVideoLauncher.launch(arrayOf("video/*")) },
                            modifier = Modifier.testTag("open_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = "Open Video File",
                                tint = CyanNeon
                            )
                        }

                        IconButton(
                            onClick = { openFolderLauncher.launch(null) },
                            modifier = Modifier.testTag("import_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Import Folder",
                                tint = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleGridView() },
                            modifier = Modifier.testTag("view_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle Grid/List",
                                tint = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setSortSheetVisible(true) },
                            modifier = Modifier.testTag("sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort Videos",
                                tint = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.refreshMedia() },
                            modifier = Modifier.testTag("refresh_button")
                        ) {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = CyanNeon
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Scan Storage",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                )

                // Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    containerColor = DarkSurface,
                    contentColor = CyanNeon,
                    edgePadding = 12.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                            color = CyanNeon,
                            height = 2.5.dp
                        )
                    }
                ) {
                    LibraryTab.values().forEach { tab ->
                        val count = when (tab) {
                            LibraryTab.ALL -> allVideos.size
                            LibraryTab.FOLDERS -> folders.size
                            LibraryTab.RECENT -> recentlyPlayed.size
                            LibraryTab.FAVORITES -> favorites.size
                            LibraryTab.HIGH_RES -> allVideos.count { it.resolutionCategory.isUltraHighRes }
                        }
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.onTabSelected(tab) },
                            text = {
                                Text(
                                    text = "${tab.title} ($count)",
                                    fontSize = 13.sp,
                                    fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (uiState.selectedTab == tab) CyanNeon else TextSecondary
                                )
                            },
                            modifier = Modifier.testTag("tab_${tab.name}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hardware Decoding Capabilities Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Intelligent Progressive Decoding Engine",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "HW: Up to ${if (uiState.deviceDecoderProfile.supports8KHardware) "8K" else if (uiState.deviceDecoderProfile.supports4KHardware) "4K" else "1080p"} • Software Fallback ready for 16K & unsupported codecs",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Resolution Filter Chips (when applicable)
            if (uiState.selectedTab == LibraryTab.ALL || uiState.selectedTab == LibraryTab.HIGH_RES) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedResolutionFilter == null,
                        onClick = { viewModel.setResolutionFilter(null) },
                        label = { Text("All", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanDark,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceCard,
                            labelColor = TextSecondary
                        )
                    )

                    listOf(
                        ResolutionCategory.ULTRA_16K,
                        ResolutionCategory.UHD_8K,
                        ResolutionCategory.UHD_4K,
                        ResolutionCategory.FHD_1080P,
                        ResolutionCategory.HD_720P
                    ).forEach { resCat ->
                        FilterChip(
                            selected = uiState.selectedResolutionFilter == resCat,
                            onClick = {
                                viewModel.setResolutionFilter(
                                    if (uiState.selectedResolutionFilter == resCat) null else resCat
                                )
                            },
                            label = { Text(resCat.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = resCat.badgeColor,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // Folder Drilldown Header
            if (uiState.selectedFolder != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .clickable { viewModel.selectFolder(null) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to folders",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Folder: ${uiState.selectedFolder?.name}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${videos.size} videos",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Main Content Area
            if (uiState.selectedTab == LibraryTab.FOLDERS && uiState.selectedFolder == null) {
                // Folder List View
                if (folders.isEmpty() && !uiState.isScanning) {
                    EmptyLibraryView(
                        title = "No Video Folders Found",
                        subtitle = "Use 'Open File' or 'Import Folder' to select videos from local storage or external SD card.",
                        onOpenFile = { openVideoLauncher.launch(arrayOf("video/*")) },
                        onImportFolder = { openFolderLauncher.launch(null) },
                        onScan = { viewModel.refreshMedia() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("folders_list"),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(folders, key = { it.bucketId }) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = { viewModel.selectFolder(folder) }
                            )
                        }
                    }
                }
            } else {
                // Video Grid / List View
                if (videos.isEmpty() && !uiState.isScanning) {
                    EmptyLibraryView(
                        title = if (uiState.searchQuery.isNotEmpty()) "No Videos Matched \"${uiState.searchQuery}\"" else "No Videos in Library",
                        subtitle = "Scan device storage, or use Storage Access Framework to play 4K, 8K, 16K video files directly.",
                        onOpenFile = { openVideoLauncher.launch(arrayOf("video/*")) },
                        onImportFolder = { openFolderLauncher.launch(null) },
                        onScan = { viewModel.refreshMedia() }
                    )
                } else {
                    if (uiState.isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("videos_grid"),
                            contentPadding = PaddingValues(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(videos, key = { it.id }) { video ->
                                VideoCard(
                                    video = video,
                                    isGridView = true,
                                    onClick = { onVideoClick(video) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(video.id) },
                                    onInfoClick = { viewModel.selectVideoForDetails(video) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("videos_list"),
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(videos, key = { it.id }) { video ->
                                VideoCard(
                                    video = video,
                                    isGridView = false,
                                    onClick = { onVideoClick(video) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(video.id) },
                                    onInfoClick = { viewModel.selectVideoForDetails(video) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modals & Bottom Sheets
        if (uiState.selectedVideoForDetails != null) {
            VideoDetailsModal(
                video = uiState.selectedVideoForDetails,
                onDismiss = { viewModel.selectVideoForDetails(null) },
                onPlayClick = { video ->
                    viewModel.selectVideoForDetails(null)
                    onVideoClick(video)
                }
            )
        }

        if (uiState.isSortSheetVisible) {
            SortBottomSheet(
                currentSortOrder = uiState.sortOrder,
                onSortSelected = { viewModel.setSortOrder(it) },
                onDismiss = { viewModel.setSortSheetVisible(false) }
            )
        }
    }
}

@Composable
fun EmptyLibraryView(
    title: String,
    subtitle: String,
    onOpenFile: () -> Unit,
    onImportFolder: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("empty_library_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.linearGradient(listOf(DarkSurfaceVariant, DarkSurfaceCard)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = CyanNeon,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenFile,
                colors = ButtonDefaults.buttonColors(containerColor = CyanDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("empty_open_file_button")
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Video", fontSize = 13.sp)
            }

            Button(
                onClick = onScan,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("empty_scan_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = CyanNeon)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Storage", fontSize = 13.sp, color = TextPrimary)
            }
        }
    }
}
