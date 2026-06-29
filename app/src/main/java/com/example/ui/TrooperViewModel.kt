package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest

enum class TrooperTab {
    HOME, EXPLORE, CREATE, CHATS, PROFILE
}

class TrooperViewModel(private val repository: TrooperRepository) : ViewModel() {

    // Tab & Navigation State
    var currentTab by mutableStateOf(TrooperTab.HOME)
        private set

    var selectedPostDetailId by mutableStateOf<Int?>(null)
    var activeStoryIndex by mutableStateOf<Int?>(null)
    var activeChatUsername by mutableStateOf<String?>(null)
    var commentSectionPostId by mutableStateOf<Int?>(null)
    var viewedProfileUsername by mutableStateOf<String?>(null)

    // UI Input States
    var searchFieldText by mutableStateOf("")
    var createCaptionText by mutableStateOf("")
    var createSelectedImageUrl by mutableStateOf("")
    var editDisplayNameText by mutableStateOf("")
    var editBioText by mutableStateOf("")
    var editAvatarUrlText by mutableStateOf("")
    var isEditingBio by mutableStateOf(false)
    var newCommentText by mutableStateOf("")

    // News Feed Control Mode (chronological, engagement)
    var feedFilterMode by mutableStateOf("chronological")
    var isDarkMode by mutableStateOf(false)

    // Auth states for login / sign up
    var authUsername by mutableStateOf("")
    var authPassword by mutableStateOf("")
    var authDisplayName by mutableStateOf("")
    var authBio by mutableStateOf("")
    var authAvatarUrl by mutableStateOf("")
    var authIsSignUp by mutableStateOf(false)
    var authErrorMessage by mutableStateOf<String?>(null)

    // Chat Message state
    var chatInputText by mutableStateOf("")
    var isBotTyping by mutableStateOf(false)
        private set

    // Preset Image URLs for creating a new Trooper Post
    val presetImages = listOf(
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600", // Futuristic Neon Abstract
        "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=600", // Hologram Mesh
        "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600", // High-Tech Motherboard
        "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600", // Deep Space Orbit
        "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600", // Cyberpunk City Lights
        "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=600", // Android Robotics
        "https://images.unsplash.com/photo-1547082299-de196ea013d6?w=600"  // Command Console
    )

    init {
        // Default select first preset image
        createSelectedImageUrl = presetImages.first()
        
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // Reactive Flows from Room
    val allProfiles: StateFlow<List<TrooperProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPosts: StateFlow<List<TrooperPost>> = repository.allPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStories: StateFlow<List<TrooperStory>> = repository.allStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserProfile: StateFlow<TrooperProfile?> = repository.currentUserProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allFollows: StateFlow<List<Follow>> = repository.allFollows
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val savedPosts: StateFlow<List<SavedPost>> = currentUserProfile.flatMapLatest { user ->
        if (user != null) repository.getSavedPosts(user.username) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<TrooperNotification>> = currentUserProfile.flatMapLatest { user ->
        if (user != null) repository.getNotifications(user.username) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Personalized News Feed based on follow relations
    val homeFeedPosts: StateFlow<List<TrooperPost>> = combine(
        allPosts,
        allFollows,
        currentUserProfile,
        snapshotFlow { feedFilterMode }
    ) { posts, follows, currentUser, filterMode ->
        if (currentUser == null) return@combine emptyList()
        
        val followingUsernames = follows
            .filter { it.followerUsername == currentUser.username }
            .map { it.followingUsername }
            .toSet()

        // Filter: own posts + followed users' posts
        val feedPosts = posts.filter {
            it.authorUsername == currentUser.username || followingUsernames.contains(it.authorUsername)
        }

        if (filterMode == "engagement") {
            feedPosts.sortedByDescending { it.likesCount }
        } else {
            feedPosts.sortedByDescending { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Explore screen - all posts sorted by engagement (trending), or curated content
    val explorePosts: StateFlow<List<TrooperPost>> = allPosts
        .map { posts -> posts.sortedByDescending { it.likesCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Suggested accounts to follow (excluding self and already followed users)
    val suggestedAccounts: StateFlow<List<TrooperProfile>> = combine(
        allProfiles,
        allFollows,
        currentUserProfile
    ) { profiles, follows, currentUser ->
        if (currentUser == null) return@combine profiles
        
        val followingUsernames = follows
            .filter { it.followerUsername == currentUser.username }
            .map { it.followingUsername }
            .toSet()

        profiles.filter {
            it.username != currentUser.username && !followingUsernames.contains(it.username)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search results by username or displayName
    val searchResults: StateFlow<List<TrooperProfile>> = combine(
        allProfiles,
        snapshotFlow { searchFieldText }
    ) { profiles, query ->
        if (query.trim().isEmpty()) {
            emptyList()
        } else {
            profiles.filter {
                it.username.contains(query, ignoreCase = true) ||
                it.displayName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow of messages for the currently active DM thread
    val activeChatMessages: StateFlow<List<DirectMessage>> = snapshotFlow { activeChatUsername }
        .flatMapLatest { username ->
            if (username != null) {
                repository.getMessagesForChat(username)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow of comments for the currently open comments screen
    val activePostComments: StateFlow<List<PostComment>> = snapshotFlow { commentSectionPostId }
        .flatMapLatest { postId ->
            if (postId != null) {
                repository.getCommentsForPost(postId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation triggers
    fun navigateToTab(tab: TrooperTab) {
        currentTab = tab
        // Clear secondary screens when changing tabs
        selectedPostDetailId = null
        activeStoryIndex = null
        activeChatUsername = null
        commentSectionPostId = null
    }

    // Action methods
    fun likePost(postId: Int) {
        viewModelScope.launch {
            repository.likePost(postId)
            val post = repository.dao.getPostById(postId) ?: return@launch
            val user = currentUserProfile.value ?: return@launch
            if (post.isLiked) {
                repository.addNotification(
                    username = user.username,
                    title = "Post Liked",
                    message = "You liked @${post.authorUsername}'s post!"
                )
                if (post.authorUsername != user.username) {
                    repository.addNotification(
                        username = post.authorUsername,
                        title = "New Like Received",
                        message = "@${user.username} liked your post!"
                    )
                }
            }
        }
    }

    fun submitComment(postId: Int) {
        if (newCommentText.trim().isEmpty()) return
        val text = newCommentText
        newCommentText = ""
        viewModelScope.launch {
            repository.addComment(postId, text)
            val post = repository.dao.getPostById(postId) ?: return@launch
            val user = currentUserProfile.value ?: return@launch
            repository.addNotification(
                username = user.username,
                title = "Comment Added",
                message = "You commented on @${post.authorUsername}'s post."
            )
            if (post.authorUsername != user.username) {
                repository.addNotification(
                    username = post.authorUsername,
                    title = "New Comment Received",
                    message = "@${user.username} commented on your post!"
                )
            }
        }
    }

    // Follow / Unfollow toggle
    fun toggleFollow(targetUsername: String) {
        val currentUser = currentUserProfile.value ?: return
        val followsList = allFollows.value
        val isFollowing = followsList.any {
            it.followerUsername == currentUser.username && it.followingUsername == targetUsername
        }
        viewModelScope.launch {
            if (isFollowing) {
                repository.unfollowUser(currentUser.username, targetUsername)
                repository.addNotification(
                    username = currentUser.username,
                    title = "Unfollowed User",
                    message = "You stopped following @$targetUsername."
                )
            } else {
                repository.followUser(currentUser.username, targetUsername)
                repository.addNotification(
                    username = currentUser.username,
                    title = "User Followed",
                    message = "You are now following @$targetUsername!"
                )
                repository.addNotification(
                    username = targetUsername,
                    title = "New Follower",
                    message = "@${currentUser.username} is now following you!"
                )
            }
        }
    }

    fun toggleSavePost(postId: Int) {
        val user = currentUserProfile.value ?: return
        viewModelScope.launch {
            val isSaved = savedPosts.value.any { it.postId == postId }
            if (isSaved) {
                repository.unsavePost(user.username, postId)
            } else {
                repository.savePost(user.username, postId)
                repository.addNotification(
                    username = user.username,
                    title = "Post Saved",
                    message = "You saved a post to your bookmarks."
                )
            }
        }
    }

    fun markAllNotificationsRead() {
        val user = currentUserProfile.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsRead(user.username)
        }
    }

    // Authentication Handlers
    fun handleAuth() {
        authErrorMessage = null
        val usernameTrimmed = authUsername.trim()
        val passwordTrimmed = authPassword.trim()

        if (usernameTrimmed.isEmpty() || passwordTrimmed.isEmpty()) {
            authErrorMessage = "Username and Password cannot be empty."
            return
        }

        // Validate username formatting
        if (!usernameTrimmed.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            authErrorMessage = "Username can only contain letters, numbers, and underscores (no spaces)."
            return
        }

        // Validate username length
        if (usernameTrimmed.length < 3) {
            authErrorMessage = "Username must be at least 3 characters long."
            return
        }

        // Validate password length
        if (passwordTrimmed.length < 6) {
            authErrorMessage = "Access Key / Password must be at least 6 characters long."
            return
        }
        
        viewModelScope.launch {
            if (authIsSignUp) {
                if (authDisplayName.trim().isEmpty()) {
                    authErrorMessage = "Display Name cannot be empty."
                    return@launch
                }
                if (authDisplayName.trim().length > 30) {
                    authErrorMessage = "Display Name cannot exceed 30 characters."
                    return@launch
                }
                if (authBio.trim().length > 150) {
                    authErrorMessage = "Bio cannot exceed 150 characters."
                    return@launch
                }
                val defaultAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
                val avatar = if (authAvatarUrl.trim().isEmpty()) defaultAvatar else authAvatarUrl
                val success = repository.signUp(
                    username = usernameTrimmed.lowercase(),
                    displayName = authDisplayName.trim(),
                    bio = authBio.trim(),
                    avatarUrl = avatar,
                    password = authPassword
                )
                if (!success) {
                    authErrorMessage = "Username is already taken."
                } else {
                    // Reset fields
                    authUsername = ""
                    authPassword = ""
                    authDisplayName = ""
                    authBio = ""
                    authAvatarUrl = ""
                }
            } else {
                val success = repository.logIn(usernameTrimmed.lowercase(), authPassword)
                if (!success) {
                    authErrorMessage = "Invalid Username or Password."
                } else {
                    // Reset fields
                    authUsername = ""
                    authPassword = ""
                }
            }
        }
    }

    fun handleGoogleSignIn(email: String, name: String) {
        authErrorMessage = null
        val defaultAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
        viewModelScope.launch {
            repository.logInWithGoogle(email, name, defaultAvatar)
        }
    }

    fun logOut() {
        viewModelScope.launch {
            repository.logOut()
            navigateToTab(TrooperTab.HOME)
        }
    }

    // Profile Edit Dialog Actions
    fun startEditBio(currentBio: String) {
        val user = currentUserProfile.value ?: return
        editDisplayNameText = user.displayName
        editBioText = user.bio
        editAvatarUrlText = user.avatarUrl
        profileEditError = null
        isEditingBio = true
    }

    var profileEditError by mutableStateOf<String?>(null)

    fun saveBio() {
        profileEditError = null
        if (editDisplayNameText.trim().isEmpty()) {
            profileEditError = "Display Name cannot be empty."
            return
        }
        if (editDisplayNameText.trim().length > 30) {
            profileEditError = "Display Name cannot exceed 30 characters."
            return
        }
        if (editBioText.trim().length > 150) {
            profileEditError = "Bio cannot exceed 150 characters."
            return
        }
        isEditingBio = false
        viewModelScope.launch {
            repository.updateProfile(editDisplayNameText.trim(), editBioText.trim(), editAvatarUrlText.trim())
            val user = currentUserProfile.value
            if (user != null) {
                repository.addNotification(
                    username = user.username,
                    title = "Profile Updated",
                    message = "Your profile details have been successfully updated."
                )
            }
        }
    }

    var createPostError by mutableStateOf<String?>(null)

    fun createPost() {
        createPostError = null
        val captionTrimmed = createCaptionText.trim()
        if (captionTrimmed.isEmpty()) {
            createPostError = "Caption cannot be empty."
            return
        }
        if (captionTrimmed.length > 500) {
            createPostError = "Caption cannot exceed 500 characters."
            return
        }
        viewModelScope.launch {
            val user = currentUserProfile.value ?: return@launch
            val newPost = TrooperPost(
                authorUsername = user.username,
                authorDisplayName = user.displayName,
                authorAvatarUrl = user.avatarUrl,
                imageUrl = createSelectedImageUrl,
                caption = captionTrimmed
            )
            repository.insertPost(newPost)
            
            // Trigger local notification
            repository.addNotification(
                username = user.username,
                title = "Post Shared",
                message = "Your post has been successfully shared."
            )
            
            // Reset input states
            createCaptionText = ""
            createSelectedImageUrl = presetImages.first()
            
            // Navigate back to Home
            navigateToTab(TrooperTab.HOME)
        }
    }

    fun createStory(imageUrl: String) {
        viewModelScope.launch {
            val user = currentUserProfile.value ?: return@launch
            val newStory = TrooperStory(
                authorUsername = user.username,
                authorAvatarUrl = user.avatarUrl,
                imageUrl = imageUrl
            )
            repository.insertStory(newStory)
        }
    }

    fun sendDirectMessage() {
        val chatUser = activeChatUsername ?: return
        val text = chatInputText
        if (text.trim().isEmpty()) return
        chatInputText = ""
        
        viewModelScope.launch {
            // 1. Send the user's message
            repository.sendMessage(chatUser, text)
            
            // 2. Set loading typing state
            isBotTyping = true
            
            // 3. Retrieve messages and profiles to construct prompt
            val messagesList = repository.getMessagesForChat(chatUser).first()
            val botProfile = allProfiles.value.find { it.username == chatUser }
            
            val systemPrompt = """
                You are ${botProfile?.displayName ?: "a platform member"} (@$chatUser), a real person in a modern social network.
                Your personality/bio is: "${botProfile?.bio ?: "A friendly social media user."}"
                Always respond in character. Keep your messages relatively short (1-3 sentences) as they are direct chat messages.
                Talk about everyday life, hobbies, work, social feed, and connect warmly. Do not use generic assistant boilerplate.
            """.trimIndent()
            
            // 4. Generate response via Repository (calls Gemini or falls back beautifully)
            val reply = repository.generateChatReply(chatUser, systemPrompt, messagesList)
            
            // 5. Insert bot reply
            repository.receiveMessage(chatUser, chatUser, reply)
            
            isBotTyping = false
        }
    }

    fun sendMessageToUser(targetUsername: String, text: String) {
        viewModelScope.launch {
            repository.sendMessage(targetUsername, text)
            
            isBotTyping = true
            val messagesList = repository.getMessagesForChat(targetUsername).first()
            val botProfile = allProfiles.value.find { it.username == targetUsername }
            val systemPrompt = """
                You are ${botProfile?.displayName ?: "a platform member"} (@$targetUsername), a real person in a modern social network.
                Your personality/bio is: "${botProfile?.bio ?: "A friendly social media user."}"
                Always respond in character. Keep your messages relatively short (1-3 sentences) as they are direct chat messages.
                Talk about everyday life, hobbies, work, social feed, and connect warmly. Do not use generic assistant boilerplate.
            """.trimIndent()
            val reply = repository.generateChatReply(targetUsername, systemPrompt, messagesList)
            repository.receiveMessage(targetUsername, targetUsername, reply)
            isBotTyping = false
        }
    }
}

// ViewModel Factory
class TrooperViewModelFactory(private val repository: TrooperRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrooperViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrooperViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
