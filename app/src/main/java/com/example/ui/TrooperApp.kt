package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

fun parseImageMetadata(url: String): Triple<String, String, Float> {
    var filter = "Normal"
    var overlay = ""
    var aspectRatio = 1.0f // default square
    
    if (url.contains("&&filter=")) {
        filter = url.substringAfter("&&filter=").substringBefore("&&")
    }
    if (url.contains("&&overlay=")) {
        overlay = url.substringAfter("&&overlay=").substringBefore("&&").replace("%20", " ")
    }
    if (url.contains("&&aspectRatio=")) {
        val aspectStr = url.substringAfter("&&aspectRatio=").substringBefore("&&")
        aspectRatio = aspectStr.toFloatOrNull() ?: 1.0f
    }
    return Triple(filter, overlay, aspectRatio)
}

fun getColorFilterForName(filterName: String): ColorFilter? {
    return when (filterName) {
        "Warm Noir" -> {
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )))
        }
        "Cyber Neon" -> {
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.2f, 0.2f, 0.2f, 0f, 20f,
                0.1f, 1.0f, 0.4f, 0f, 10f,
                0.4f, 0.1f, 1.5f, 0f, 30f,
                0f,   0f,   0f,   1f, 0f
            )))
        }
        "Vintage Slate" -> {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.2f) })
        }
        "Sunset Glow" -> {
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.3f, 0.1f, 0.1f, 0f, 10f,
                0.1f, 1.1f, 0.1f, 0f, 5f,
                0.1f, 0.1f, 0.8f, 0f, 0f,
                0f,   0f,   0f,   1f, 0f
            )))
        }
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrooperApp(viewModel: TrooperViewModel) {
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val posts by viewModel.allPosts.collectAsStateWithLifecycle()
    val stories by viewModel.allStories.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUserProfile.collectAsStateWithLifecycle()

    val user = currentUser
    if (user == null) {
        AuthScreen(viewModel = viewModel)
    } else {
        Scaffold(
            bottomBar = {
                TrooperBottomNavigation(
                    currentTab = viewModel.currentTab,
                    onTabSelected = { viewModel.navigateToTab(it) },
                    currentUserAvatar = user.avatarUrl
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                // Main Content Switcher
                AnimatedContent(
                    targetState = viewModel.currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "MainContent"
                ) { tab ->
                    when (tab) {
                        TrooperTab.HOME -> {
                            val homePosts by viewModel.homeFeedPosts.collectAsStateWithLifecycle()
                            HomeScreen(
                                viewModel = viewModel,
                                posts = homePosts,
                                stories = stories,
                                currentUser = user
                            )
                        }
                        TrooperTab.EXPLORE -> {
                            val explorePosts by viewModel.explorePosts.collectAsStateWithLifecycle()
                            ExploreScreen(
                                viewModel = viewModel,
                                posts = explorePosts
                            )
                        }
                        TrooperTab.CREATE -> {
                            CreatePostScreen(
                                viewModel = viewModel
                            )
                        }
                        TrooperTab.CHATS -> {
                            ChatsScreen(
                                viewModel = viewModel,
                                profiles = profiles
                            )
                        }
                        TrooperTab.PROFILE -> {
                            ProfileScreen(
                                viewModel = viewModel,
                                posts = posts,
                                currentUser = user
                            )
                        }
                    }
                }

            // Overlay Detail Screens
            viewModel.viewedProfileUsername?.let { username ->
                val profile = profiles.find { it.username == username }
                if (profile != null) {
                    AccountDetailScreen(
                        profile = profile,
                        viewModel = viewModel,
                        posts = posts,
                        onDismiss = { viewModel.viewedProfileUsername = null }
                    )
                }
            }

            viewModel.selectedPostDetailId?.let { postId ->
                val post = posts.find { it.id == postId }
                if (post != null) {
                    PostDetailScreen(
                        post = post,
                        viewModel = viewModel,
                        onDismiss = { viewModel.selectedPostDetailId = null }
                    )
                }
            }

            viewModel.activeStoryIndex?.let { storyIndex ->
                if (storyIndex in stories.indices) {
                    StoryViewerDialog(
                        stories = stories,
                        initialIndex = storyIndex,
                        viewModel = viewModel,
                        onDismiss = { viewModel.activeStoryIndex = null }
                    )
                }
            }

            viewModel.commentSectionPostId?.let { postId ->
                val post = posts.find { it.id == postId }
                if (post != null) {
                    CommentsDialog(
                        post = post,
                        viewModel = viewModel,
                        onDismiss = { viewModel.commentSectionPostId = null }
                    )
                }
            }
        }
    }
}
}

// ==========================================
// Bottom Navigation Component
// ==========================================
@Composable
fun TrooperBottomNavigation(
    currentTab: TrooperTab,
    onTabSelected: (TrooperTab) -> Unit,
    currentUserAvatar: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp
    ) {
        Column {
            Divider(color = TrooperBorder, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                IconButton(
                    onClick = { onTabSelected(TrooperTab.HOME) },
                    modifier = Modifier.testTag("nav_home_tab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = if (currentTab == TrooperTab.HOME) TrooperOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Explore
                IconButton(
                    onClick = { onTabSelected(TrooperTab.EXPLORE) },
                    modifier = Modifier.testTag("nav_explore_tab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Explore",
                        tint = if (currentTab == TrooperTab.EXPLORE) TrooperOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Create
                IconButton(
                    onClick = { onTabSelected(TrooperTab.CREATE) },
                    modifier = Modifier.testTag("nav_create_tab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create",
                        tint = if (currentTab == TrooperTab.CREATE) TrooperOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Chats
                IconButton(
                    onClick = { onTabSelected(TrooperTab.CHATS) },
                    modifier = Modifier.testTag("nav_chats_tab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Chats",
                        tint = if (currentTab == TrooperTab.CHATS) TrooperOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Profile Tab Avatar
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = if (currentTab == TrooperTab.PROFILE) TrooperOrange else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onTabSelected(TrooperTab.PROFILE) }
                        .testTag("nav_profile_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = currentUserAvatar,
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

// ==========================================
// Custom Icons & Drawing
// ==========================================
@Composable
fun CommentIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(24.dp)) {
        val path = Path().apply {
            // Draw chat bubble
            moveTo(3f, 4f)
            lineTo(21f, 4f)
            quadraticTo(22f, 4f, 22f, 5f)
            lineTo(22f, 15f)
            quadraticTo(22f, 16f, 21f, 16f)
            lineTo(8f, 16f)
            lineTo(3f, 21f)
            lineTo(3f, 16f)
            quadraticTo(2f, 16f, 2f, 15f)
            lineTo(2f, 5f)
            quadraticTo(2f, 4f, 3f, 4f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// ==========================================
// Home Screen Component
// ==========================================
@Composable
fun HomeScreen(
    viewModel: TrooperViewModel,
    posts: List<TrooperPost>,
    stories: List<TrooperStory>,
    currentUser: TrooperProfile?
) {
    var showAddStoryDialog by remember { mutableStateOf(false) }
    var selectedStoryPreset by remember { mutableStateOf(viewModel.presetImages.firstOrNull() ?: "") }

    if (showAddStoryDialog) {
        Dialog(onDismissRequest = { showAddStoryDialog = false }) {
            var storyFilter by remember { mutableStateOf("Normal") }
            var storyOverlay by remember { mutableStateOf("") }
            val dialogScrollState = rememberScrollState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TrooperDarkGray)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                        .verticalScroll(dialogScrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CREATE NEW STORY",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrooperOrange,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Selected Preset Preview (Shows filter and overlay live!)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedStoryPreset.isNotEmpty()) {
                            AsyncImage(
                                model = selectedStoryPreset,
                                contentDescription = "Preset Preview",
                                contentScale = ContentScale.Crop,
                                colorFilter = getColorFilterForName(storyFilter),
                                modifier = Modifier.fillMaxSize()
                            )

                            if (storyOverlay.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = storyOverlay,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Text("No background selected", color = TrooperGray, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    MediaPickerControlsRow(
                        onImageSelected = { localPath ->
                            selectedStoryPreset = localPath
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Select Story Background:",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Presets selection row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.presetImages) { imageUrl ->
                            val isSelected = selectedStoryPreset == imageUrl
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) TrooperOrange else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedStoryPreset = imageUrl }
                            ) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Story option",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filters selector
                    Text(
                        text = "Select Filter:",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filters = listOf("Normal", "Warm Noir", "Cyber Neon", "Vintage Slate", "Sunset Glow")
                        items(filters) { filter ->
                            CustomEditChip(
                                selected = storyFilter == filter,
                                label = filter,
                                onClick = { storyFilter = filter }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Overlay Text input
                    Text(
                        text = "Add Text Overlay on Story:",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = storyOverlay,
                        onValueChange = { storyOverlay = it },
                        placeholder = { Text("Overlay text...", color = TrooperGray, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrooperOrange,
                            unfocusedBorderColor = TrooperBorder,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            cursorColor = TrooperOrange
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddStoryDialog = false },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, TrooperBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                        ) {
                            Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (selectedStoryPreset.isNotEmpty()) {
                                    val finalStoryUrl = "${selectedStoryPreset}&&filter=$storyFilter&&overlay=${storyOverlay.replace(" ", "%20")}&&"
                                    viewModel.createStory(finalStoryUrl)
                                    showAddStoryDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = TrooperOrange)
                        ) {
                            Text("SHARE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showNotificationsDialog by remember { mutableStateOf(false) }
    val notificationList by viewModel.notifications.collectAsStateWithLifecycle()
    val hasUnread = notificationList.any { !it.isRead }
    val savedList by viewModel.savedPosts.collectAsStateWithLifecycle()

    if (showNotificationsDialog) {
        Dialog(onDismissRequest = { showNotificationsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 450.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TACTICAL COMMS BELL",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrooperOrange,
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = { showNotificationsDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (notificationList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No notifications received.",
                                color = TrooperGray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(notificationList) { notification ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (notification.isRead) Color.Transparent else TrooperOrange)
                                                .align(Alignment.CenterVertically)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = notification.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = notification.message,
                                                fontSize = 12.sp,
                                                color = TrooperGray
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = TrooperBorder, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.markAllNotificationsRead()
                            showNotificationsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrooperOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("MARK ALL AS READ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TROOPERS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp // Clean tracking-tighter look from Design HTML
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sync / Pull to Refresh Button
                IconButton(
                    onClick = {
                        scope.launch {
                            isRefreshing = true
                            delay(1200)
                            isRefreshing = false
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Sci-Fi Dark Mode Indicator Pill
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (viewModel.isDarkMode) TrooperOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                        .border(1.dp, if (viewModel.isDarkMode) TrooperOrange else TrooperBorder, RoundedCornerShape(4.dp))
                        .clickable { viewModel.isDarkMode = !viewModel.isDarkMode }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (viewModel.isDarkMode) "SYS: DARK" else "SYS: LIGHT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.isDarkMode) TrooperOrange else MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.5.sp
                    )
                }

                // Notifications bell
                Box {
                    IconButton(
                        onClick = { showNotificationsDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (hasUnread) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TrooperOrange)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.navigateToTab(TrooperTab.CREATE) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Post",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box {
                    IconButton(
                        onClick = { viewModel.navigateToTab(TrooperTab.CHATS) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "DMs",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Divider(color = TrooperBorder, thickness = 0.5.dp)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (isRefreshing) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TrooperOrange)
                    }
                }
            }
            // Stories Section
            item {
                StoriesRow(
                    stories = stories,
                    onStoryClick = { index ->
                        viewModel.activeStoryIndex = index
                    },
                    onAddStoryClick = {
                        showAddStoryDialog = true
                    }
                )
                Divider(color = TrooperBorder, thickness = 0.5.dp)
            }

            // News Feed Sorting Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort Feed:",
                        fontSize = 12.sp,
                        color = TrooperGray,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    FilterChip(
                        selected = viewModel.feedFilterMode == "chronological",
                        onClick = { viewModel.feedFilterMode = "chronological" },
                        label = { Text("Recent Briefs", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TrooperOrange.copy(alpha = 0.15f),
                            selectedLabelColor = TrooperOrange,
                            containerColor = Color.Transparent,
                            labelColor = TrooperGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewModel.feedFilterMode == "chronological",
                            borderColor = if (viewModel.feedFilterMode == "chronological") TrooperOrange else TrooperBorder,
                            selectedBorderColor = TrooperOrange,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        )
                    )

                    FilterChip(
                        selected = viewModel.feedFilterMode == "engagement",
                        onClick = { viewModel.feedFilterMode = "engagement" },
                        label = { Text("Trending Actions", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TrooperOrange.copy(alpha = 0.15f),
                            selectedLabelColor = TrooperOrange,
                            containerColor = Color.Transparent,
                            labelColor = TrooperGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewModel.feedFilterMode == "engagement",
                            borderColor = if (viewModel.feedFilterMode == "engagement") TrooperOrange else TrooperBorder,
                            selectedBorderColor = TrooperOrange,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        )
                    )
                }
                Divider(color = TrooperBorder, thickness = 0.5.dp)
            }

            // Feed Posts List
            if (posts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your Feed is Empty",
                            textAlign = TextAlign.Center,
                            color = TrooperOrange,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Follow other users to see their posts in your personalized feed!",
                            textAlign = TextAlign.Center,
                            color = TrooperGray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "RECOMMENDED CREATORS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                // Show suggested accounts list directly in empty feed
                val suggestedList = viewModel.suggestedAccounts.value
                if (suggestedList.isNotEmpty()) {
                    items(suggestedList) { profile ->
                        SuggestedCreatorItem(profile = profile, viewModel = viewModel)
                    }
                } else {
                    item {
                        Text(
                            text = "No creators currently online.",
                            color = TrooperGray,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(posts) { post ->
                    val isSaved = savedList.any { it.postId == post.id }
                    FeedPostItem(
                        post = post,
                        isSaved = isSaved,
                        onLikeClick = { viewModel.likePost(post.id) },
                        onCommentClick = { viewModel.commentSectionPostId = post.id },
                        onSaveClick = { viewModel.toggleSavePost(post.id) },
                        onProfileClick = {
                            if (post.authorUsername == currentUser?.username) {
                                viewModel.navigateToTab(TrooperTab.PROFILE)
                            } else {
                                viewModel.viewedProfileUsername = post.authorUsername
                            }
                        }
                    )
                }
            }
        }
    }
}

// Horizontal Stories List
@Composable
fun StoriesRow(
    stories: List<TrooperStory>,
    onStoryClick: (Int) -> Unit,
    onAddStoryClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User's own add story
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.clickable { onAddStoryClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(TrooperDarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Story",
                        tint = TrooperOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Your Story", color = TrooperGray, fontSize = 11.sp)
            }
        }

        items(stories.size) { index ->
            val story = stories[index]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onStoryClick(index) }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = ImmersiveActiveGradient
                            ),
                            shape = CircleShape
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = story.authorAvatarUrl,
                        contentDescription = story.authorUsername,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.authorUsername,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(64.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Single Feed Post Composable
@Composable
fun FeedPostItem(
    post: TrooperPost,
    isSaved: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    var showLikeHeart by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (showLikeHeart) 1.2f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "HeartScale"
    )
    val opacity by animateFloatAsState(
        targetValue = if (showLikeHeart) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "HeartOpacity"
    )

    LaunchedEffect(showLikeHeart) {
        if (showLikeHeart) {
            delay(600)
            showLikeHeart = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        // 1. Post Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                AsyncImage(
                    model = post.authorAvatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = post.authorUsername,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Sector Patrol",
                        color = TrooperOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Verified",
                tint = TrooperOrange,
                modifier = Modifier.size(16.dp)
            )
        }

        // 2. Post Image (Fills width, customizable aspect) with double tap like
        val (imgFilter, imgOverlay, imgAspect) = parseImageMetadata(post.imageUrl)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(imgAspect)
                .background(TrooperDarkGray)
                .pointerInput(post.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (!post.isLiked) {
                                onLikeClick()
                            }
                            showLikeHeart = true
                        }
                    )
                }
        ) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = "Post Image",
                contentScale = ContentScale.Crop,
                colorFilter = getColorFilterForName(imgFilter),
                modifier = Modifier.fillMaxSize()
            )

            if (imgOverlay.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = imgOverlay,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (scale > 0f) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Liked",
                    tint = TrooperOrange,
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.Center)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            alpha = opacity
                        )
                )
            }
        }

        // 3. Post Actions Bottom Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Like Button
                IconButton(onClick = onLikeClick, modifier = Modifier.testTag("post_like_button_${post.id}")) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) TrooperOrange else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Comment Button
                IconButton(onClick = onCommentClick, modifier = Modifier.testTag("post_comment_button_${post.id}")) {
                    CommentIcon(color = MaterialTheme.colorScheme.onBackground)
                }

                // Share Button
                IconButton(onClick = { /* Share placeholder */ }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Save / Bookmark Button
            IconButton(onClick = onSaveClick, modifier = Modifier.testTag("post_save_button_${post.id}")) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Save Post",
                    tint = if (isSaved) TrooperOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 4. Likes and Captions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = "${String.format("%,d", post.likesCount)} likes",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${post.authorUsername} ",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = post.caption,
                    color = TrooperLightGray,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // View all comments clickable
            Text(
                text = "View all comments",
                color = TrooperGray,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { onCommentClick() }
                    .testTag("view_comments_${post.id}")
            )
        }
    }
}

// ==========================================
// Explore Screen Component
// ==========================================
@Composable
fun ExploreScreen(
    viewModel: TrooperViewModel,
    posts: List<TrooperPost>
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val suggestedAccounts by viewModel.suggestedAccounts.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar Row with Sync
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TrooperDarkGray)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TrooperGray,
                        modifier = Modifier.size(18.dp)
                    )
                    BasicTextField(
                        value = viewModel.searchFieldText,
                        onValueChange = { viewModel.searchFieldText = it },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth().testTag("user_search_input"),
                        decorationBox = { innerTextField ->
                            if (viewModel.searchFieldText.isEmpty()) {
                                Text("Search troopers by username or full name...", color = TrooperGray, fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            IconButton(
                onClick = {
                    scope.launch {
                        isRefreshing = true
                        delay(1200)
                        isRefreshing = false
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Feed",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (isRefreshing) {
            LinearProgressIndicator(color = TrooperOrange, modifier = Modifier.fillMaxWidth())
        }

        if (viewModel.searchFieldText.isNotEmpty()) {
            // Search Results Mode
            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No profiles found matching \"${viewModel.searchFieldText}\"", color = TrooperGray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResults) { profile ->
                        SearchProfileResultItem(profile = profile, viewModel = viewModel)
                    }
                }
            }
        } else {
            // Standard Explore / Discover Mode
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Discover suggested accounts section in the grid
                if (suggestedAccounts.isNotEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "SUGGESTED FOR YOU",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TrooperOrange,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(suggestedAccounts) { profile ->
                                    ExploreSuggestedAccountCard(profile = profile, viewModel = viewModel)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = TrooperBorder, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "TRENDING SECTOR ACTIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Grid items for trending posts
                items(posts) { post ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(TrooperDarkGray)
                            .clickable { viewModel.selectedPostDetailId = post.id }
                    ) {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = "Grid Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Likes",
                                    tint = TrooperOrange,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${post.likesCount}",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom styled interactive editor chips
@Composable
fun CustomEditChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) TrooperOrange.copy(alpha = 0.2f) else TrooperDarkGray)
            .border(
                width = 1.dp,
                color = if (selected) TrooperOrange else TrooperBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) TrooperOrange else MaterialTheme.colorScheme.onBackground,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==========================================
// Create Post Screen with Live Visual Editing
// ==========================================
@Composable
fun CreatePostScreen(viewModel: TrooperViewModel) {
    var selectedFilter by remember { mutableStateOf("Normal") }
    var selectedOverlay by remember { mutableStateOf("") }
    var selectedAspect by remember { mutableStateOf("1.0") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CREATE NEW POST",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TrooperOrange,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Selected Image Preview (Adjusts aspect ratio live!)
        val aspectFloat = selectedAspect.toFloatOrNull() ?: 1.0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectFloat)
                .clip(RoundedCornerShape(12.dp))
                .background(TrooperDarkGray)
                .border(1.dp, TrooperBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = viewModel.createSelectedImageUrl,
                contentDescription = "Selected Image Preview",
                contentScale = ContentScale.Crop,
                colorFilter = getColorFilterForName(selectedFilter),
                modifier = Modifier.fillMaxSize()
            )

            // Render text overlay live on preview!
            if (selectedOverlay.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = selectedOverlay,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        MediaPickerControlsRow(
            onImageSelected = { localPath ->
                viewModel.createSelectedImageUrl = localPath
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Preset images horizontal row selector
        Text(
            text = "Select Image Preset:",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.presetImages) { imageUrl ->
                val isSelected = viewModel.createSelectedImageUrl == imageUrl
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 2.dp,
                            color = if (isSelected) TrooperOrange else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.createSelectedImageUrl = imageUrl }
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Preset option",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image Aspect Ratio Selector
        Text(
            text = "Aspect Ratio:",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val aspects = listOf("1.0" to "Square 1:1", "0.8" to "Portrait 4:5", "1.77" to "Widescreen 16:9", "1.33" to "Classic 4:3")
            aspects.forEach { (value, label) ->
                CustomEditChip(
                    selected = selectedAspect == value,
                    label = label,
                    onClick = { selectedAspect = value }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Image Filters Row
        Text(
            text = "Photo Filter:",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("Normal", "Warm Noir", "Cyber Neon", "Vintage Slate", "Sunset Glow")
            items(filters) { filter ->
                CustomEditChip(
                    selected = selectedFilter == filter,
                    label = filter,
                    onClick = { selectedFilter = filter }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Image Text Overlay Field
        Text(
            text = "Add Text Overlay on Photo:",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = selectedOverlay,
            onValueChange = { selectedOverlay = it },
            placeholder = { Text("Type overlay text...", color = TrooperGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrooperOrange,
                unfocusedBorderColor = TrooperBorder,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedPlaceholderColor = TrooperGray,
                unfocusedPlaceholderColor = TrooperGray,
                cursorColor = TrooperOrange
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_post_overlay_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Caption text area
        Text(
            text = "Caption:",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = viewModel.createCaptionText,
            onValueChange = { viewModel.createCaptionText = it },
            placeholder = { Text("Write a caption for this post...", color = TrooperGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrooperOrange,
                unfocusedBorderColor = TrooperBorder,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedPlaceholderColor = TrooperGray,
                unfocusedPlaceholderColor = TrooperGray,
                cursorColor = TrooperOrange
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .testTag("create_caption_input")
        )

        Spacer(modifier = Modifier.height(24.dp))

        viewModel.createPostError?.let { error ->
            Text(
                text = error,
                color = Color.Red,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Share button
        Button(
            onClick = {
                val finalUrl = "${viewModel.createSelectedImageUrl}&&filter=$selectedFilter&&overlay=${selectedOverlay.replace(" ", "%20")}&&aspectRatio=$selectedAspect&&"
                viewModel.createSelectedImageUrl = finalUrl
                viewModel.createPost()
            },
            colors = ButtonDefaults.buttonColors(containerColor = TrooperOrange),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("create_post_submit_button"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Send, contentDescription = "Publish", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SHARE POST", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ==========================================
// Chats Screen Component (Gemini character bot chat!)
// ==========================================
@Composable
fun ChatsScreen(
    viewModel: TrooperViewModel,
    profiles: List<TrooperProfile>
) {
    val activeChat = viewModel.activeChatUsername

    if (activeChat == null) {
        // Chat list screen
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MILITARY COMLINK",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrooperOrange,
                    letterSpacing = 1.sp
                )
            }
            Divider(color = TrooperBorder, thickness = 0.5.dp)

            val chatBots = profiles.filter { !it.isCurrentUser }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(chatBots) { bot ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.activeChatUsername = bot.username }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(54.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            AsyncImage(
                                model = bot.avatarUrl,
                                contentDescription = bot.username,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                            // Green online indicator
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(TrooperAccent)
                                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bot.displayName,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Active now",
                                color = TrooperOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Open Chat",
                            tint = TrooperGray
                        )
                    }
                    Divider(color = TrooperBorder, thickness = 0.5.dp)
                }
            }
        }
    } else {
        // Active direct chat room screen
        val botProfile = profiles.find { it.username == activeChat }
        val messages by viewModel.activeChatMessages.collectAsStateWithLifecycle()

        Column(modifier = Modifier.fillMaxSize()) {
            // Chat Room Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.activeChatUsername = null }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (botProfile != null) {
                    AsyncImage(
                        model = botProfile.avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = botProfile.displayName,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "HOLOGRAPHIC COMLINK ACTIVE",
                            color = TrooperAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            Divider(color = TrooperBorder, thickness = 0.5.dp)

            // Chat Messages Body List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    val isUser = msg.senderUsername != activeChat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isUser) 12.dp else 0.dp,
                                        bottomEnd = if (isUser) 0.dp else 12.dp
                                    )
                                )
                                .background(if (isUser) TrooperOrange else TrooperDarkGray)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .widthIn(max = 260.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // AI Transmitting indicator
                if (viewModel.isBotTyping) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = "Transmitting secure holographic reply...",
                                color = TrooperOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            // Message Input bar
            Divider(color = TrooperBorder, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.chatInputText,
                    onValueChange = { viewModel.chatInputText = it },
                    placeholder = { Text("Transmit secure message...", color = TrooperGray, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrooperOrange,
                        unfocusedBorderColor = TrooperBorder,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        cursorColor = TrooperOrange
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text"),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.sendDirectMessage() },
                    modifier = Modifier.testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = TrooperOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// Profile Screen Component
// ==========================================
@Composable
fun ProfileScreen(
    viewModel: TrooperViewModel,
    posts: List<TrooperPost>,
    currentUser: TrooperProfile?
) {
    if (currentUser == null) return

    val myPosts = posts.filter { it.authorUsername == currentUser.username }
    val follows by viewModel.allFollows.collectAsStateWithLifecycle()
    
    // Calculate REAL counts based on Follows table
    val followerCount = follows.count { it.followingUsername == currentUser.username }
    val followingCount = follows.count { it.followerUsername == currentUser.username }

    Column(modifier = Modifier.fillMaxSize()) {
        // Profile Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentUser.username,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            // Log Out Button
            IconButton(
                onClick = { viewModel.logOut() },
                modifier = Modifier.testTag("logout_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = TrooperOrange
                )
            }
        }

        // Stats & Photo Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Profile photo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(2.dp, TrooperOrange, CircleShape)
                    .padding(4.dp)
            ) {
                AsyncImage(
                    model = currentUser.avatarUrl,
                    contentDescription = "My Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            // Stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileStatItem(count = myPosts.size.toString(), label = "Posts")
                ProfileStatItem(count = String.format("%,d", followerCount), label = "Followers")
                ProfileStatItem(count = followingCount.toString(), label = "Following")
            }
        }

        // Bio section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = currentUser.displayName,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentUser.bio,
                color = TrooperLightGray,
                fontSize = 13.sp
            )
        }

        // Edit profile fields row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            if (viewModel.isEditingBio) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Display Name", color = TrooperOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = viewModel.editDisplayNameText,
                        onValueChange = { viewModel.editDisplayNameText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_display_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrooperOrange,
                            unfocusedBorderColor = TrooperBorder,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    
                    Text("Tactical Bio", color = TrooperOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = viewModel.editBioText,
                        onValueChange = { viewModel.editBioText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_bio_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrooperOrange,
                            unfocusedBorderColor = TrooperBorder,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    Text("Profile Picture URL", color = TrooperOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = viewModel.editAvatarUrlText,
                        onValueChange = { viewModel.editAvatarUrlText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_avatar_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrooperOrange,
                            unfocusedBorderColor = TrooperBorder,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    MediaPickerControlsRow(
                        onImageSelected = { localPath ->
                            viewModel.editAvatarUrlText = localPath
                        }
                    )

                    viewModel.profileEditError?.let { error ->
                        Text(
                            text = error,
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.isEditingBio = false }
                        ) {
                            Text("Cancel", color = TrooperGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.saveBio() },
                            colors = ButtonDefaults.buttonColors(containerColor = TrooperOrange),
                            modifier = Modifier.testTag("save_bio_button")
                        ) {
                            Text("Save Profile", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.startEditBio(currentUser.bio) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_bio_button"),
                    border = BorderStroke(1.dp, TrooperBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    Text("Edit profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = TrooperBorder, thickness = 0.5.dp)

        // Grid of posts
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(myPosts) { post ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(TrooperDarkGray)
                        .clickable { viewModel.selectedPostDetailId = post.id }
                ) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Grid Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = label, color = TrooperGray, fontSize = 12.sp)
    }
}

// ==========================================
// Dialog: Comments Sheet View
// ==========================================
@Composable
fun CommentsDialog(
    post: TrooperPost,
    viewModel: TrooperViewModel,
    onDismiss: () -> Unit
) {
    val comments by viewModel.activePostComments.collectAsStateWithLifecycle()

    LaunchedEffect(post.id) {
        viewModel.commentSectionPostId = post.id
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = TrooperDarkGray),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tactical Comms Log", color = TrooperOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Divider(color = TrooperBorder)

                // List of comments
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // Original post caption as header comment
                    item {
                        Row {
                            AsyncImage(
                                model = post.authorAvatarUrl,
                                contentDescription = post.authorUsername,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("@${post.authorUsername}", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(post.caption, color = TrooperLightGray, fontSize = 13.sp)
                            }
                        }
                        Divider(color = TrooperBorder, modifier = Modifier.padding(vertical = 12.dp))
                    }

                    if (comments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No comments yet.", color = TrooperGray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(comments) { comment ->
                            Row {
                                AsyncImage(
                                    model = comment.authorAvatarUrl,
                                    contentDescription = comment.authorUsername,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("@${comment.authorUsername}", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(comment.text, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                Divider(color = TrooperBorder)

                // Add comment entry
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewModel.newCommentText,
                        onValueChange = { viewModel.newCommentText = it },
                        placeholder = { Text("Log a squad reply...", color = TrooperGray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrooperOrange,
                            unfocusedBorderColor = TrooperBorder,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_comment_input"),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.submitComment(post.id) },
                        modifier = Modifier.testTag("submit_comment_button")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Transmit", tint = TrooperOrange)
                    }
                }
            }
        }
    }
}

// ==========================================
// Dialog: Story Viewer Dialog
// ==========================================
@Composable
fun StoryViewerDialog(
    stories: List<TrooperStory>,
    initialIndex: Int,
    viewModel: TrooperViewModel,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }
    val story = stories.getOrNull(currentIndex) ?: return

    var progress by remember { mutableStateOf(0f) }
    var replyText by remember { mutableStateOf("") }
    var isLiked by remember { mutableStateOf(false) }

    LaunchedEffect(currentIndex, replyText.isNotEmpty()) {
        if (replyText.isNotEmpty()) return@LaunchedEffect // Pause timer when replying
        progress = 0f
        while (progress < 1f) {
            delay(50)
            progress += 0.01f
        }
        if (currentIndex < stories.size - 1) {
            currentIndex++
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Background main story photo (non-clickable)
            AsyncImage(
                model = story.imageUrl,
                contentDescription = "Story Media",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Transparent dual-zone touch overlay (occupying 75% height to leave room for bottom reply bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            ) {
                // Left 35% touch area -> Previous story
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentIndex > 0) {
                                currentIndex--
                            } else {
                                progress = 0f
                            }
                        }
                )
                
                // Right 65% touch area -> Next story
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentIndex < stories.size - 1) {
                                currentIndex++
                            } else {
                                onDismiss()
                            }
                        }
                )
            }

            // Header layer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Top Progress bar indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { idx, _ ->
                        val itemProgress = when {
                            idx < currentIndex -> 1f
                            idx == currentIndex -> progress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = itemProgress,
                            color = TrooperOrange,
                            trackColor = Color.DarkGray,
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = story.authorAvatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(story.authorUsername, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Transmission Story", color = TrooperGray, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // Bottom reply and action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.Transparent),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Reply to story...", color = TrooperGray, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrooperOrange,
                        unfocusedBorderColor = TrooperBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("story_reply_input"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 1,
                    trailingIcon = {
                        if (replyText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.sendMessageToUser(
                                        story.authorUsername,
                                        "Replied to story: \"$replyText\""
                                    )
                                    replyText = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Transmit Reply",
                                    tint = TrooperOrange
                                )
                            }
                        }
                    }
                )

                IconButton(
                    onClick = {
                        isLiked = !isLiked
                        if (isLiked) {
                            viewModel.sendMessageToUser(
                                story.authorUsername,
                                "❤️ Liked your story!"
                            )
                        }
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Story",
                        tint = if (isLiked) Color.Red else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// Dialog: Post Detail Screen Dialog
// ==========================================
@Composable
fun PostDetailScreen(
    post: TrooperPost,
    viewModel: TrooperViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = TrooperDarkGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(post.authorUsername, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Post Detail Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(post.caption, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.commentSectionPostId = post.id
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrooperOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OPEN SQUAD COMMS LOG", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// Authentication screen and helper modules
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: TrooperViewModel) {
    var showGoogleDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Name & Logo
            Text(
                text = "TROOPERS",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TrooperOrange,
                letterSpacing = (-1).sp
            )
            
            Text(
                text = "A fast, modern social network for squad members.",
                fontSize = 13.sp,
                color = TrooperGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auth Error
            viewModel.authErrorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = Color.Red,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Input fields
            OutlinedTextField(
                value = viewModel.authUsername,
                onValueChange = { viewModel.authUsername = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("auth_username_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrooperOrange,
                    unfocusedBorderColor = TrooperBorder,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = TrooperOrange
                )
            )

            OutlinedTextField(
                value = viewModel.authPassword,
                onValueChange = { viewModel.authPassword = it },
                label = { Text("Access Key / Password") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrooperOrange,
                    unfocusedBorderColor = TrooperBorder,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = TrooperOrange
                )
            )

            if (viewModel.authIsSignUp) {
                OutlinedTextField(
                    value = viewModel.authDisplayName,
                    onValueChange = { viewModel.authDisplayName = it },
                    label = { Text("Tactical Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("auth_display_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrooperOrange,
                        unfocusedBorderColor = TrooperBorder,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        cursorColor = TrooperOrange
                    )
                )

                OutlinedTextField(
                    value = viewModel.authBio,
                    onValueChange = { viewModel.authBio = it },
                    label = { Text("Bio (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("auth_bio_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrooperOrange,
                        unfocusedBorderColor = TrooperBorder,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        cursorColor = TrooperOrange
                    )
                )

                OutlinedTextField(
                    value = viewModel.authAvatarUrl,
                    onValueChange = { viewModel.authAvatarUrl = it },
                    label = { Text("Profile Pic URL (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("auth_avatar_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrooperOrange,
                        unfocusedBorderColor = TrooperBorder,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        cursorColor = TrooperOrange
                    )
                )
            }

            // Submit Button
            Button(
                onClick = { viewModel.handleAuth() },
                colors = ButtonDefaults.buttonColors(containerColor = TrooperOrange),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_submit_button"),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = if (viewModel.authIsSignUp) "Create Organic Profile" else "Access Secure Feed",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Toggle Login / Signup
            TextButton(
                onClick = { 
                    viewModel.authIsSignUp = !viewModel.authIsSignUp 
                    viewModel.authErrorMessage = null
                },
                modifier = Modifier.testTag("auth_toggle_button")
            ) {
                Text(
                    text = if (viewModel.authIsSignUp) "Already registered? Sign In" else "New to Troopers? Register Profile",
                    color = TrooperOrange,
                    fontSize = 13.sp
                )
            }

            Divider(color = TrooperBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

            // Sign In with Google option (MANDATORY per user request)
            OutlinedButton(
                onClick = { showGoogleDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("google_signin_button"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, TrooperBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "G ",
                        color = TrooperOrange,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign in with Google",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }

    if (showGoogleDialog) {
        Dialog(onDismissRequest = { showGoogleDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TrooperDarkGray),
                border = BorderStroke(1.dp, TrooperBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Google Account Chooser",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    
                    Text(
                        text = "Confirm account to sign in securely to Troopers:",
                        fontSize = 12.sp,
                        color = TrooperGray,
                        textAlign = TextAlign.Center
                    )

                    // Simulated Google Account Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable {
                                viewModel.handleGoogleSignIn(
                                    email = "turboabner3@gmail.com",
                                    name = "Turbo Abner"
                                )
                                showGoogleDialog = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Turbo Abner",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "turboabner3@gmail.com",
                                fontSize = 12.sp,
                                color = TrooperGray
                            )
                        }
                    }

                    TextButton(
                        onClick = { showGoogleDialog = false }
                    ) {
                        Text("Cancel", color = TrooperGray)
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedCreatorItem(
    profile: TrooperProfile,
    viewModel: TrooperViewModel
) {
    val follows by viewModel.allFollows.collectAsStateWithLifecycle()
    val isFollowing = follows.any { 
        it.followerUsername == (viewModel.currentUserProfile.value?.username ?: "") && 
        it.followingUsername == profile.username 
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = TrooperDarkGray),
        border = BorderStroke(0.5.dp, TrooperBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, TrooperBorder, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = profile.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "@${profile.username}",
                        fontSize = 12.sp,
                        color = TrooperGray
                    )
                }
            }
            
            Button(
                onClick = { viewModel.toggleFollow(profile.username) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) TrooperDarkGray else TrooperOrange,
                    contentColor = if (isFollowing) TrooperOrange else Color.Black
                ),
                border = if (isFollowing) BorderStroke(1.dp, TrooperOrange) else null,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SearchProfileResultItem(
    profile: TrooperProfile,
    viewModel: TrooperViewModel
) {
    val follows by viewModel.allFollows.collectAsStateWithLifecycle()
    val isFollowing = follows.any { 
        it.followerUsername == (viewModel.currentUserProfile.value?.username ?: "") && 
        it.followingUsername == profile.username 
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.viewedProfileUsername = profile.username
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, TrooperBorder, CircleShape)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = profile.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "@${profile.username}",
                    fontSize = 13.sp,
                    color = TrooperGray
                )
            }
        }

        Button(
            onClick = { viewModel.toggleFollow(profile.username) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFollowing) TrooperDarkGray else TrooperOrange,
                contentColor = if (isFollowing) TrooperOrange else Color.Black
            ),
            border = if (isFollowing) BorderStroke(1.dp, TrooperOrange) else null,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = if (isFollowing) "Following" else "Follow",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ExploreSuggestedAccountCard(
    profile: TrooperProfile,
    viewModel: TrooperViewModel
) {
    val follows by viewModel.allFollows.collectAsStateWithLifecycle()
    val isFollowing = follows.any { 
        it.followerUsername == (viewModel.currentUserProfile.value?.username ?: "") && 
        it.followingUsername == profile.username 
    }

    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable {
                viewModel.viewedProfileUsername = profile.username
            },
        colors = CardDefaults.cardColors(containerColor = TrooperDarkGray),
        border = BorderStroke(0.5.dp, TrooperBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(1.dp, TrooperBorder, CircleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${profile.username}",
                fontSize = 10.sp,
                color = TrooperGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { viewModel.toggleFollow(profile.username) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) Color.Transparent else TrooperOrange,
                    contentColor = if (isFollowing) TrooperOrange else Color.Black
                ),
                border = if (isFollowing) BorderStroke(0.5.dp, TrooperOrange) else null,
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(26.dp).fillMaxWidth()
            ) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// Account Detail Screen (Profile Viewer Overlay)
// ==========================================
@Composable
fun AccountDetailScreen(
    profile: TrooperProfile,
    viewModel: TrooperViewModel,
    posts: List<TrooperPost>,
    onDismiss: () -> Unit
) {
    val authorPosts = posts.filter { it.authorUsername == profile.username }
    val follows by viewModel.allFollows.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    
    val followerCount = follows.count { it.followingUsername == profile.username }
    val followingCount = follows.count { it.followerUsername == profile.username }
    val isFollowing = follows.any { 
        it.followerUsername == (currentUser?.username ?: "") && it.followingUsername == profile.username 
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COMMAND BRIEF: @${profile.username}",
                        color = TrooperOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (profile.username != currentUser?.username) {
                        // Direct Message button
                        IconButton(
                            onClick = {
                                onDismiss()
                                viewModel.activeChatUsername = profile.username
                                viewModel.navigateToTab(TrooperTab.CHATS)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Send DM",
                                tint = TrooperOrange
                            )
                        }
                    }
                }
                
                Divider(color = TrooperBorder, thickness = 0.5.dp)

                // Stats & Photo Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(2.dp, TrooperOrange, CircleShape)
                            .padding(4.dp)
                    ) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "${profile.displayName}'s Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileStatItem(count = authorPosts.size.toString(), label = "Posts")
                        ProfileStatItem(count = String.format("%,d", followerCount), label = "Followers")
                        ProfileStatItem(count = followingCount.toString(), label = "Following")
                    }
                }

                // Bio
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = profile.displayName,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile.bio,
                        color = TrooperLightGray,
                        fontSize = 13.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (profile.username != currentUser?.username) {
                        // Follow / Unfollow button
                        Button(
                            onClick = { viewModel.toggleFollow(profile.username) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) TrooperDarkGray else TrooperOrange
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isFollowing) "DISCONNECT COMLINK" else "ESTABLISH SECURE COMLINK",
                                fontWeight = FontWeight.Bold,
                                color = if (isFollowing) MaterialTheme.colorScheme.onBackground else Color.White,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = TrooperBorder, thickness = 0.5.dp)

                // Post Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(1.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(authorPosts) { post ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(TrooperDarkGray)
                                .clickable { 
                                    onDismiss()
                                    viewModel.selectedPostDetailId = post.id 
                                }
                        ) {
                            AsyncImage(
                                model = post.imageUrl,
                                contentDescription = "Grid Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaPickerControlsRow(
    onImageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = saveUriToLocalFile(context, uri)
            if (localPath != null) {
                onImageSelected(localPath)
            }
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val localPath = saveBitmapToLocalFile(context, bitmap)
            if (localPath != null) {
                onImageSelected(localPath)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = { cameraLauncher.launch() },
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, TrooperBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TrooperOrange),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Take Photo")
            Spacer(modifier = Modifier.width(8.dp))
            Text("TAKE PHOTO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, TrooperBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TrooperOrange),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Gallery Upload")
            Spacer(modifier = Modifier.width(8.dp))
            Text("CHOOSE GALLERY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun saveUriToLocalFile(context: android.content.Context, uri: Uri): String? {
    return try {
        val contentResolver = context.contentResolver
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val file = File(context.filesDir, "trooper_img_${UUID.randomUUID()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            "file://${file.absolutePath}"
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveBitmapToLocalFile(context: android.content.Context, bitmap: Bitmap): String? {
    return try {
        val file = File(context.filesDir, "trooper_img_${UUID.randomUUID()}.jpg")
        val outputStream = FileOutputStream(file)
        outputStream.use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        "file://${file.absolutePath}"
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

