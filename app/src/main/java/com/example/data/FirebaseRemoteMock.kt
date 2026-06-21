package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

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
    private const val DEFAULT_DATA_VERSION = 7
    private val seedFeedIds = setOf("feed_1", "feed_2", "feed_3", "feed_4")
    
    // Default active group when the app starts
    var activeGroupId: String = "PNUCS1"
    
    val defaultGroups = listOf(
        GroupMock(
            groupId = "PNUCS1",
            name = "부산대 컴공 21대 회장단",
            inviteCode = "PNUCS1",
            memberIds = listOf("user_me", "user_ronaldo", "user_karina", "user_kangdongwon")
        ),
        GroupMock(
            groupId = "PNUART",
            name = "부산대 미공개 씹인싸단 🎨",
            inviteCode = "PNUART",
            memberIds = listOf("user_me", "user_somin", "user_taewoo")
        )
    )

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val feedListAdapterType = Types.newParameterizedType(List::class.java, FeedMock::class.java)
    private val groupListAdapterType = Types.newParameterizedType(List::class.java, GroupMock::class.java)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun ensureDefaultDataIsCurrent(context: Context) {
        val prefs = getPrefs(context)
        if (prefs.getInt("default_data_version", 0) == DEFAULT_DATA_VERSION) return

        val existingGroups = prefs.getString("groups_json", null)
            ?.let { runCatching { moshi.adapter<List<GroupMock>>(groupListAdapterType).fromJson(it) }.getOrNull() }
            .orEmpty()
        val existingFeeds = prefs.getString("feeds_json", null)
            ?.let { runCatching { moshi.adapter<List<FeedMock>>(feedListAdapterType).fromJson(it) }.getOrNull() }
            .orEmpty()

        val defaultGroupIds = defaultGroups.map { it.groupId }.toSet()
        val mergedGroups = defaultGroups + existingGroups.filterNot { it.groupId in defaultGroupIds }

        val freshSeedFeeds = generateInitialFeeds()
        val preservedUserFeeds = existingFeeds.filterNot { it.feedId in seedFeedIds }
        val mergedFeeds = preservedUserFeeds + freshSeedFeeds

        prefs.edit()
            .putString("groups_json", moshi.adapter<List<GroupMock>>(groupListAdapterType).toJson(mergedGroups))
            .putString("feeds_json", moshi.adapter<List<FeedMock>>(feedListAdapterType).toJson(mergedFeeds))
            .putInt("default_data_version", DEFAULT_DATA_VERSION)
            .apply()
    }

    fun getGroups(context: Context): List<GroupMock> {
        ensureDefaultDataIsCurrent(context)
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
        ensureDefaultDataIsCurrent(context)
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
        val pnuCsTarget = getGroupTargetEmotion("PNUCS1", today)
        val ronaldoEmotion = mapOf("HAPPY" to 0.37713462f, "SAD" to 0.32255632f, "ANGRY" to 0.1728333f, "SURPRISED" to 0.12747572f)
        val karinaEmotion = mapOf("HAPPY" to 0.8749612f, "SAD" to 0.095444135f, "ANGRY" to 0.012166077f, "SURPRISED" to 0.017428614f)
        val kangDongwonEmotion = mapOf("HAPPY" to 0.92413455f, "SAD" to 0.01157314f, "ANGRY" to 0.013893702f, "SURPRISED" to 0.05039858f)
        return listOf(
            FeedMock(
                feedId = "feed_1",
                userId = "user_ronaldo",
                userName = "부산대 호날두",
                userProfileEmoji = "⚽",
                groupId = "PNUCS1",
                date = today,
                photoUrl = "android.resource://com.aistudio.happybutsad.pnu/drawable/pnu_ronaldo",
                targetEmotion = pnuCsTarget,
                resultEmotion = ronaldoEmotion,
                score = calculateSeedScore(pnuCsTarget, ronaldoEmotion),
                latitude = 35.2334,
                longitude = 129.0792,
                address = "부산대 새벽삼거리 숲길"
            ),
            FeedMock(
                feedId = "feed_2",
                userId = "user_karina",
                userName = "부산대 카리나",
                userProfileEmoji = "💎",
                groupId = "PNUCS1",
                date = today,
                photoUrl = "android.resource://com.aistudio.happybutsad.pnu/drawable/pnu_karina",
                targetEmotion = pnuCsTarget,
                resultEmotion = karinaEmotion,
                score = calculateSeedScore(pnuCsTarget, karinaEmotion),
                latitude = 35.2312,
                longitude = 129.0831,
                address = "부산대 넉넉한터 스탠드"
            ),
            FeedMock(
                feedId = "feed_3",
                userId = "user_kangdongwon",
                userName = "부산대 강동원",
                userProfileEmoji = "🎬",
                groupId = "PNUCS1",
                date = today,
                photoUrl = "android.resource://com.aistudio.happybutsad.pnu/drawable/pnu_kangdongwon",
                targetEmotion = pnuCsTarget,
                resultEmotion = kangDongwonEmotion,
                score = calculateSeedScore(pnuCsTarget, kangDongwonEmotion),
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
                photoUrl = "https://picsum.photos/seed/pnu-somin-art/640/480",
                targetEmotion = mapOf("HAPPY" to 0.3f, "SURPRISED" to 0.7f),
                resultEmotion = mapOf("HAPPY" to 0.3f, "SURPRISED" to 0.7f),
                score = 94.1f,
                latitude = 35.2324,
                longitude = 129.0772,
                address = "부산대학교 예술대학 조소실"
            )
        )
    }

    private fun calculateSeedScore(target: Map<String, Float>, result: Map<String, Float>): Float {
        val emotions = listOf("HAPPY", "SAD", "ANGRY", "SURPRISED")
        val distance = sqrt(
            emotions.sumOf { emotion ->
                val diff = (target[emotion] ?: 0f) - (result[emotion] ?: 0f)
                (diff * diff).toDouble()
            }
        ).toFloat()
        return ((1f - distance) * 100f).coerceIn(0f, 100f)
    }
    // Helper to generate Today's Target Emotion mixture stably based on Date
    fun getDailyTargetEmotion(dateString: String): Map<String, Float> {
        val hash = dateString.hashCode()
        val random = Random(hash.toLong())
        val emotions = listOf("HAPPY", "SAD", "ANGRY", "SURPRISED")
        
        val weights = emotions.map { 1 + random.nextInt(10) }
        val sum = weights.sum().toFloat()
        
        return emotions.zip(weights.map { it / sum }).toMap()
    }

    // Helper to generate Group-Specific Target Emotion mixture stably based on Group ID + Date
    fun getGroupTargetEmotion(groupId: String, dateString: String): Map<String, Float> {
        if (groupId == "PNUCS1") {
            return mapOf("HAPPY" to 0.7f, "SAD" to 0.1f, "ANGRY" to 0.1f, "SURPRISED" to 0.1f)
        }

        val hash = (groupId + dateString).hashCode()
        val random = Random(hash.toLong())
        val emotions = listOf("HAPPY", "SAD", "ANGRY", "SURPRISED")
        
        val weights = emotions.map { 1 + random.nextInt(10) }
        val sum = weights.sum().toFloat()
        
        return emotions.zip(weights.map { it / sum }).toMap()
    }
}

