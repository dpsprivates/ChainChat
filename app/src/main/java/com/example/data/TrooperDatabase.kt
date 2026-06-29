package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. Entities
// ==========================================

@Entity(tableName = "trooper_profiles")
data class TrooperProfile(
    @PrimaryKey val username: String,
    val displayName: String,
    val avatarUrl: String,
    val bio: String,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isCurrentUser: Boolean = false,
    val password: String = "password"
)

@Entity(tableName = "follows", primaryKeys = ["followerUsername", "followingUsername"])
data class Follow(
    val followerUsername: String,
    val followingUsername: String
)

@Entity(tableName = "trooper_posts")
data class TrooperPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String,
    val imageUrl: String,
    val caption: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLiked: Boolean = false
)

@Entity(tableName = "post_comments")
data class PostComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "trooper_stories")
data class TrooperStory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorUsername: String,
    val authorAvatarUrl: String,
    val imageUrl: String,
    val isViewed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "direct_messages")
data class DirectMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatUsername: String, // The other person's username (key for the DM thread)
    val senderUsername: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_posts", primaryKeys = ["username", "postId"])
data class SavedPost(
    val username: String,
    val postId: Int
)

@Entity(tableName = "notifications")
data class TrooperNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

// ==========================================
// 2. DAO (Unified for simplicity & performance)
// ==========================================

@Dao
interface TrooperDao {
    // Profiles
    @Query("SELECT * FROM trooper_profiles")
    fun getAllProfilesFlow(): Flow<List<TrooperProfile>>

    @Query("SELECT * FROM trooper_profiles WHERE username = :username LIMIT 1")
    suspend fun getProfile(username: String): TrooperProfile?

    @Query("SELECT * FROM trooper_profiles WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserProfileFlow(): Flow<TrooperProfile?>

    @Query("SELECT * FROM trooper_profiles WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUserProfile(): TrooperProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: TrooperProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<TrooperProfile>)

    // Posts
    @Query("SELECT * FROM trooper_posts ORDER BY timestamp DESC")
    fun getAllPostsFlow(): Flow<List<TrooperPost>>

    @Query("SELECT * FROM trooper_posts WHERE id = :postId")
    suspend fun getPostById(postId: Int): TrooperPost?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: TrooperPost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<TrooperPost>)

    @Update
    suspend fun updatePost(post: TrooperPost)

    // Comments
    @Query("SELECT * FROM post_comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPostFlow(postId: Int): Flow<List<PostComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: PostComment)

    // Stories
    @Query("SELECT * FROM trooper_stories ORDER BY timestamp DESC")
    fun getAllStoriesFlow(): Flow<List<TrooperStory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: TrooperStory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<TrooperStory>)

    @Update
    suspend fun updateStory(story: TrooperStory)

    // Direct Messages
    @Query("SELECT * FROM direct_messages WHERE chatUsername = :chatUsername ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatUsername: String): Flow<List<DirectMessage>>

    @Query("SELECT * FROM direct_messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<DirectMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DirectMessage)

    // Follow Relations
    @Query("SELECT * FROM follows")
    fun getAllFollowsFlow(): Flow<List<Follow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: Follow)

    @Query("DELETE FROM follows WHERE followerUsername = :followerUsername AND followingUsername = :followingUsername")
    suspend fun deleteFollow(followerUsername: String, followingUsername: String)

    @Query("UPDATE trooper_profiles SET isCurrentUser = 0")
    suspend fun clearCurrentUser()

    // Saved Posts
    @Query("SELECT * FROM saved_posts WHERE username = :username")
    fun getSavedPostsFlow(username: String): Flow<List<SavedPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPost(savedPost: SavedPost)

    @Query("DELETE FROM saved_posts WHERE username = :username AND postId = :postId")
    suspend fun deleteSavedPost(username: String, postId: Int)

    // Notifications
    @Query("SELECT * FROM notifications WHERE username = :username ORDER BY timestamp DESC")
    fun getNotificationsFlow(username: String): Flow<List<TrooperNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: TrooperNotification)

    @Query("UPDATE notifications SET isRead = 1 WHERE username = :username")
    suspend fun markAllNotificationsRead(username: String)
}

// ==========================================
// 3. Database
// ==========================================

@Database(
    entities = [
        TrooperProfile::class,
        Follow::class,
        TrooperPost::class,
        PostComment::class,
        TrooperStory::class,
        DirectMessage::class,
        SavedPost::class,
        TrooperNotification::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TrooperDatabase : RoomDatabase() {
    abstract fun trooperDao(): TrooperDao

    companion object {
        @Volatile
        private var INSTANCE: TrooperDatabase? = null

        fun getDatabase(context: Context): TrooperDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrooperDatabase::class.java,
                    "troopers_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
