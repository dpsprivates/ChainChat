package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TrooperRepository(val dao: TrooperDao) {

    val allProfiles: Flow<List<TrooperProfile>> = dao.getAllProfilesFlow()
    val allPosts: Flow<List<TrooperPost>> = dao.getAllPostsFlow()
    val allStories: Flow<List<TrooperStory>> = dao.getAllStoriesFlow()
    val currentUserProfile: Flow<TrooperProfile?> = dao.getCurrentUserProfileFlow()
    val allFollows: Flow<List<Follow>> = dao.getAllFollowsFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getCommentsForPost(postId: Int): Flow<List<PostComment>> {
        return dao.getCommentsForPostFlow(postId)
    }

    suspend fun getMessagesForChat(chatUsername: String): Flow<List<DirectMessage>> {
        return dao.getMessagesForChatFlow(chatUsername).map { list ->
            list.map { msg -> msg.copy(text = TrooperCrypto.decrypt(msg.text)) }
        }
    }

    suspend fun getAllMessages(): Flow<List<DirectMessage>> {
        return dao.getAllMessagesFlow().map { list ->
            list.map { msg -> msg.copy(text = TrooperCrypto.decrypt(msg.text)) }
        }
    }

    suspend fun insertPost(post: TrooperPost) {
        dao.insertPost(post)
    }

    suspend fun likePost(postId: Int) {
        val post = dao.getPostById(postId)
        if (post != null) {
            val updated = post.copy(
                isLiked = !post.isLiked,
                likesCount = if (post.isLiked) post.likesCount - 1 else post.likesCount + 1
            )
            dao.updatePost(updated)
        }
    }

    suspend fun addComment(postId: Int, text: String) {
        val currentUser = dao.getCurrentUserProfile() ?: return
        val comment = PostComment(
            postId = postId,
            authorUsername = currentUser.username,
            authorDisplayName = currentUser.displayName,
            authorAvatarUrl = currentUser.avatarUrl,
            text = text
        )
        dao.insertComment(comment)
    }

    suspend fun updateBio(newBio: String) {
        val currentUser = dao.getCurrentUserProfile() ?: return
        val updated = currentUser.copy(bio = newBio)
        dao.insertProfile(updated)
    }

    // Saved Posts
    fun getSavedPosts(username: String): Flow<List<SavedPost>> {
        return dao.getSavedPostsFlow(username)
    }

    suspend fun savePost(username: String, postId: Int) {
        dao.insertSavedPost(SavedPost(username, postId))
    }

    suspend fun unsavePost(username: String, postId: Int) {
        dao.deleteSavedPost(username, postId)
    }

    // Notifications
    fun getNotifications(username: String): Flow<List<TrooperNotification>> {
        return dao.getNotificationsFlow(username)
    }

    suspend fun addNotification(username: String, title: String, message: String) {
        dao.insertNotification(TrooperNotification(username = username, title = title, message = message))
    }

    suspend fun markAllNotificationsRead(username: String) {
        dao.markAllNotificationsRead(username)
    }

    suspend fun updateProfile(displayName: String, bio: String, avatarUrl: String) {
        val currentUser = dao.getCurrentUserProfile() ?: return
        val updated = currentUser.copy(displayName = displayName, bio = bio, avatarUrl = avatarUrl)
        dao.insertProfile(updated)
        
        // Also update details in user's posts
        val posts = dao.getAllPostsFlow().first()
        posts.forEach { post ->
            if (post.authorUsername == currentUser.username) {
                dao.updatePost(post.copy(
                    authorDisplayName = displayName,
                    authorAvatarUrl = avatarUrl
                ))
            }
        }
    }

    suspend fun followUser(follower: String, following: String) {
        dao.insertFollow(Follow(follower, following))
    }

    suspend fun unfollowUser(follower: String, following: String) {
        dao.deleteFollow(follower, following)
    }

    suspend fun logIn(username: String, password: String): Boolean {
        val profile = dao.getProfile(username)
        if (profile != null && profile.password == password) {
            dao.clearCurrentUser()
            dao.insertProfile(profile.copy(isCurrentUser = true))
            return true
        }
        return false
    }

    suspend fun logInWithGoogle(email: String, name: String, avatarUrl: String): TrooperProfile {
        val username = email.substringBefore("@").replace(".", "_").lowercase()
        dao.clearCurrentUser()
        val existing = dao.getProfile(username)
        val profile = if (existing != null) {
            existing.copy(isCurrentUser = true)
        } else {
            TrooperProfile(
                username = username,
                displayName = name,
                avatarUrl = avatarUrl,
                bio = "Space explorer registered via Google. 🚀",
                isCurrentUser = true,
                password = "google_auth_no_password"
            )
        }
        dao.insertProfile(profile)
        return profile
    }

    suspend fun signUp(username: String, displayName: String, bio: String, avatarUrl: String, password: String): Boolean {
        val existing = dao.getProfile(username)
        if (existing != null) return false
        
        val profile = TrooperProfile(
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            bio = bio,
            isCurrentUser = true,
            password = password
        )
        dao.clearCurrentUser()
        dao.insertProfile(profile)
        return true
    }

    suspend fun logOut() {
        dao.clearCurrentUser()
    }

    suspend fun sendMessage(chatUsername: String, text: String) {
        val currentUser = dao.getCurrentUserProfile() ?: return
        val encrypted = TrooperCrypto.encrypt(text)
        val userMsg = DirectMessage(
            chatUsername = chatUsername,
            senderUsername = currentUser.username,
            text = encrypted
        )
        dao.insertMessage(userMsg)
    }

    suspend fun receiveMessage(chatUsername: String, senderUsername: String, text: String) {
        val encrypted = TrooperCrypto.encrypt(text)
        val botMsg = DirectMessage(
            chatUsername = chatUsername,
            senderUsername = senderUsername,
            text = encrypted
        )
        dao.insertMessage(botMsg)
    }

    suspend fun insertStory(story: TrooperStory) {
        dao.insertStory(story)
    }

    suspend fun generateChatReply(botUsername: String, systemPrompt: String, chatHistory: List<DirectMessage>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Fallback to local deterministic high-quality response
            kotlinx.coroutines.delay(1500)
            return@withContext getOfflineBotReply(botUsername, chatHistory.lastOrNull()?.text ?: "")
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            
            val contentsArray = JSONArray()
            
            // System instructions
            val systemObj = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            }

            // Chat history conversion
            chatHistory.takeLast(10).forEach { msg ->
                val role = if (msg.senderUsername == botUsername) "model" else "user"
                val turn = JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.text) })
                    })
                }
                contentsArray.put(turn)
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", systemObj)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBodyJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("TrooperRepository", "Gemini call failed with code: ${response.code}")
                    return@withContext getOfflineBotReply(botUsername, chatHistory.lastOrNull()?.text ?: "")
                }
                val bodyStr = response.body?.string() ?: ""
                val jsonObj = JSONObject(bodyStr)
                val candidates = jsonObj.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No response text")
                        }
                    }
                }
                return@withContext getOfflineBotReply(botUsername, chatHistory.lastOrNull()?.text ?: "")
            }
        } catch (e: Exception) {
            Log.e("TrooperRepository", "Gemini exception: ${e.message}", e)
            return@withContext getOfflineBotReply(botUsername, chatHistory.lastOrNull()?.text ?: "")
        }
    }

    private fun getOfflineBotReply(botUsername: String, lastUserMsg: String): String {
        return when (botUsername) {
            "alex_explorer" -> {
                val responses = listOf(
                    "Signal confirmed, Commander. I've logged the travel coordinate coordinates. Let's explore the next quadrant together! 🪐",
                    "A perfect frame! The solar winds are picking up, but my photographic scanner is still holding lock. 📸",
                    "Understood. Heading back to the rover to back up these telemetry pictures. Catch you on the net. 🛸"
                )
                responses.random()
            }
            "sara_design" -> {
                val responses = listOf(
                    "Layout review: optimal. I'm tweaking the border stroke from 1dp to 2dp for better visual hierarchy! 🎨",
                    "Tactical feed UI is fully responsive. Always use TrooperOrange as our primary focal accent, copy that? 🍊",
                    "Designing a new holographic hud widget for our suit HUDs. I'll drop the transmission wireframe shortly!"
                )
                responses.random()
            }
            "tech_sam" -> {
                val responses = listOf(
                    "Telemetry packets compiled: 0% loss. End-to-end comlink encryption is running at 100%. 💻",
                    "Access key authenticated. I am running standard background audits on the tactical net. No anomalies found.",
                    "Executing loop. Your micro-transmission is received. Code is deployed and compiling successfully! 🚀"
                )
                responses.random()
            }
            "luna_nature" -> {
                val responses = listOf(
                    "Lab report: Oxygen levels are holding nominal. The cosmic seed we planted in sector 4 is sprouting beautifully! 🌿",
                    "Understood, Commander. Remember to conserve your suit hydration levels during active patrols. 💧",
                    "Fascinating ecosystem telemetry. Astrobotany scan completes in 3 minutes. Stay green! 🌸"
                )
                responses.random()
            }
            else -> "Secure comlink packet received. Standing by for next command, Commander."
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        // No placeholder/bot accounts seeded. Fully user-driven platform.
    }
}
