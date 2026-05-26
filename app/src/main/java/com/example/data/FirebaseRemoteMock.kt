package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.*

data class GroupMock(
    val groupId: String,
    val name: String,
    val inviteCode: String,
    val memberIds: List<String>,
    val createdAt: Long = System.currentTimeMillis()
)

data class FeedMock(
    val feedId: String,
    val userId: String,
    val userName: String,
    val userProfileEmoji: String,
    val groupId: String,
    val date: String,
    val photoUrl: String, // Can be local URI or styled color template for mocking
    val targetEmotion: Map<String, Float>,
    val resultEmotion: Map<String, Float>,
    val score: Float,
    val latitude: Double,
    val longitude: Double,
    val address: String = "부산대학교 정문",
    val timestamp: Long = System.currentTimeMillis()
)

object FirebaseRemoteMock {
    private const val PREFS_NAME = "pnu_firebase_mock_prefs"
    
    // Default active group when the app starts
    var activeGroupId: String = "PNUCS1"
    
    val defaultGroups = listOf(
        GroupMock(
            groupId = "PNUCS1",
            name = "부산대 컴공 21대 회장단",
            inviteCode = "PNUCS1",
            memberIds = listOf("user_me", "user_minji", "user_hyunu", "user_jiung", "user_yujin")
        ),
        GroupMock(
            groupId = "PNUART",
            name = "부산대 미공개 씹인싸단 🎨",
            inviteCode = "PNUART",
            memberIds = listOf("user_me", "user_somin", "user_taewoo")
        ),
        GroupMock(
            groupId = "PNU_CAFE",
            name = "도서관 탈출기 ☕",
            inviteCode = "PNUTRA",
            memberIds = listOf("user_me", "user_gildong", "user_heewon")
        )
    )

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val feedListAdapterType = Types.newParameterizedType(List::class.java, FeedMock::class.java)
    private val groupListAdapterType = Types.newParameterizedType(List::class.java, GroupMock::class.java)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getGroups(context: Context): List<GroupMock> {
        val prefs = getPrefs(context)
        val json = prefs.getString("groups_json", null)
        if (json == null) {
            saveGroups(context, defaultGroups)
            return defaultGroups
        }
        return try {
            moshi.adapter<List<GroupMock>>(groupListAdapterType).fromJson(json) ?: defaultGroups
        } catch (e: Exception) {
            defaultGroups
        }
    }

    fun saveGroups(context: Context, list: List<GroupMock>) {
        val json = moshi.adapter<List<GroupMock>>(groupListAdapterType).toJson(list)
        getPrefs(context).edit().putString("groups_json", json).apply()
    }

    fun getFeeds(context: Context): List<FeedMock> {
        val prefs = getPrefs(context)
        val json = prefs.getString("feeds_json", null)
        if (json == null) {
            val initial = generateInitialFeeds()
            saveFeeds(context, initial)
            return initial
        }
        return try {
            moshi.adapter<List<FeedMock>>(feedListAdapterType).fromJson(json) ?: generateInitialFeeds()
        } catch (e: Exception) {
            generateInitialFeeds()
        }
    }

    fun saveFeeds(context: Context, list: List<FeedMock>) {
        val json = moshi.adapter<List<FeedMock>>(feedListAdapterType).toJson(list)
        getPrefs(context).edit().putString("feeds_json", json).apply()
    }

    fun addFeed(context: Context, feed: FeedMock) {
        val feeds = getFeeds(context).toMutableList()
        feeds.add(0, feed)
        saveFeeds(context, feeds)
    }

    fun addGroup(context: Context, name: String, code: String): GroupMock {
        val groups = getGroups(context).toMutableList()
        val newGroup = GroupMock(
            groupId = UUID.randomUUID().toString(),
            name = name,
            inviteCode = code.uppercase(Locale.ROOT),
            memberIds = listOf("user_me")
        )
        groups.add(newGroup)
        saveGroups(context, groups)
        return newGroup
    }

    fun joinGroupByCode(context: Context, code: String): GroupMock? {
        val groups = getGroups(context).toMutableList()
        val targetIndex = groups.indexOfFirst { it.inviteCode.equals(code, ignoreCase = true) }
        if (targetIndex != -1) {
            val group = groups[targetIndex]
            if (!group.memberIds.contains("user_me")) {
                val updatedMembers = group.memberIds.toMutableList().apply { add("user_me") }
                val updatedGroup = group.copy(memberIds = updatedMembers)
                groups[targetIndex] = updatedGroup
                saveGroups(context, groups)
                return updatedGroup
            }
            return group
        }
        return null
    }

    private fun generateInitialFeeds(): List<FeedMock> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return listOf(
            FeedMock(
                feedId = "feed_1",
                userId = "user_minji",
                userName = "정문_토스트_진심녀(민지)",
                userProfileEmoji = "🦊",
                groupId = "PNUCS1",
                date = today,
                photoUrl = "temp_minji_happy",
                targetEmotion = mapOf("HAPPY" to 0.5f, "SAD" to 0.5f),
                resultEmotion = mapOf("HAPPY" to 0.45f, "SAD" to 0.42f, "NEUTRAL" to 0.13f),
                score = 88.5f,
                latitude = 35.2334,
                longitude = 129.0792,
                address = "부산대 새벽삼거리 숲길"
            ),
            FeedMock(
                feedId = "feed_2",
                userId = "user_hyunu",
                userName = "건설관_등반_달인(현우)",
                userProfileEmoji = "🐻",
                groupId = "PNUCS1",
                date = today,
                photoUrl = "temp_hyunu_sad",
                targetEmotion = mapOf("HAPPY" to 0.5f, "SAD" to 0.5f),
                resultEmotion = mapOf("HAPPY" to 0.21f, "SAD" to 0.61f, "NEUTRAL" to 0.18f),
                score = 79.2f,
                latitude = 35.2312,
                longitude = 129.0831,
                address = "부산대 넉넉한터 스탠드"
            ),
            FeedMock(
                feedId = "feed_3",
                userId = "user_jiung",
                userName = "금정산성_걸어서_정복(지웅)",
                userProfileEmoji = "🦁",
                groupId = "PNUCS1",
                date = today,
                photoUrl = "temp_jiung_neutral",
                targetEmotion = mapOf("HAPPY" to 0.5f, "SAD" to 0.5f),
                resultEmotion = mapOf("HAPPY" to 0.48f, "SAD" to 0.52f, "NEUTRAL" to 0.0f),
                score = 96.8f,
                latitude = 35.2301,
                longitude = 129.0789,
                address = "부산대 정보컴퓨터공학관 무지개관"
            ),
            FeedMock(
                feedId = "feed_4",
                userId = "user_somin",
                userName = "인스타_감성_소민",
                userProfileEmoji = "💅",
                groupId = "PNUART",
                date = today,
                photoUrl = "temp_somin_art",
                targetEmotion = mapOf("HAPPY" to 0.3f, "SURPRISED" to 0.7f),
                resultEmotion = mapOf("HAPPY" to 0.28f, "SURPRISED" to 0.65f, "NEUTRAL" to 0.07f),
                score = 94.1f,
                latitude = 35.2324,
                longitude = 129.0772,
                address = "부산대학교 예술대학 조소실"
            )
        )
    }

    // Helper to generate Today's Target Emotion mixture stably based on Date
    fun getDailyTargetEmotion(dateString: String): Map<String, Float> {
        val hash = dateString.hashCode()
        val random = Random(hash.toLong())
        val emotions = listOf("HAPPY", "SAD", "ANGRY", "SURPRISED", "NEUTRAL", "FEAR", "DISGUST")
        
        val weights = emotions.map { 1 + random.nextInt(10) }
        val sum = weights.sum().toFloat()
        
        return emotions.zip(weights.map { it / sum }).toMap()
    }

    // Helper to generate Group-Specific Target Emotion mixture stably based on Group ID + Date
    fun getGroupTargetEmotion(groupId: String, dateString: String): Map<String, Float> {
        val hash = (groupId + dateString).hashCode()
        val random = Random(hash.toLong())
        val emotions = listOf("HAPPY", "SAD", "ANGRY", "SURPRISED", "NEUTRAL", "FEAR", "DISGUST")
        
        val weights = emotions.map { 1 + random.nextInt(10) }
        val sum = weights.sum().toFloat()
        
        return emotions.zip(weights.map { it / sum }).toMap()
    }
}
