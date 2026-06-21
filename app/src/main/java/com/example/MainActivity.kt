package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.FirebaseRemoteMock
import com.example.data.MissionRepository
import com.example.network.GeminiRepository
import com.example.ui.theme.MZTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.glassBackground
import com.example.ui.theme.glassCard
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var repository: MissionRepository
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        repository = MissionRepository(database.missionDao())

        setContent {
            MyApplicationTheme {
                MainHubScreen(
                    repository = repository,
                    onStartMission = { targetJson, selectedGroupId ->
                        val intent = Intent(this, MissionActivity::class.java).apply {
                            putExtra("emotionTarget", targetJson)
                            putExtra("groupId", selectedGroupId)
                        }
                        startActivity(intent)
                    },
                    onOpenFeed = { groupId ->
                        val intent = Intent(this, GroupActivity::class.java).apply {
                            putExtra("groupId", groupId)
                        }
                        startActivity(intent)
                    },
                    onOpenArchive = { groupId ->
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val intent = Intent(this, ArchiveActivity::class.java).apply {
                            putExtra("groupId", groupId)
                            putExtra("date", todayStr)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHubScreen(
    repository: MissionRepository,
    onStartMission: (String, String) -> Unit,
    onOpenFeed: (String) -> Unit,
    onOpenArchive: (String) -> Unit
) {
    val context = LocalContext.current
    val recordsState by repository.allRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    var isDarkTheme by remember { mutableStateOf(false) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    
    val groupsList = remember { FirebaseRemoteMock.getGroups(context) }
    var selectedGroupId by remember { mutableStateOf(FirebaseRemoteMock.activeGroupId) }
    
    val todayTarget = remember(selectedGroupId, todayStr) { 
        FirebaseRemoteMock.getGroupTargetEmotion(selectedGroupId, todayStr) 
    }
    
    val targetJson = remember(todayTarget) {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Float::class.javaObjectType)
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter<Map<String, Float>>(type).toJson(todayTarget)
    }

    // Checking if today is already completed
    val isTodayCompleted = recordsState.any { it.date == todayStr }

    // AI-generated daily mission tagline (Gemini). Falls back to a fixed line
    // whenever the key is missing or the call fails, so the card is never empty.
    val fallbackTagline = "과제는 많은데 날씨가 좋아 기쁜데 슬픈 이 미묘한 마음을 담아 앞코(전면) 카메라로 표현해 보아라."
    var aiTagline by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(targetJson) {
        val emoText = todayTarget.entries.joinToString(", ") { (emo, ratio) ->
            val kor = when (emo) {
                "HAPPY" -> "기쁨"; "SAD" -> "슬픔"; "ANGRY" -> "분노"; "SURPRISED" -> "놀람"; else -> emo
            }
            "$kor ${(ratio * 100).toInt()}%"
        }
        aiTagline = GeminiRepository.generate(
            "너는 부산대학교 감정 셀카 미션 앱의 재치있는 카피라이터야. " +
                "오늘의 목표 감정 비율은 [$emoText]. 이 조합을 표현하라고 학생에게 권하는 " +
                "셀카 미션 멘트를 한 문장(40자 이내, 반말, 부산 감성)으로 만들어줘. 따옴표 없이 문장만 출력."
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .glassBackground(isDarkTheme),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "기쁜데 슬프다",
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = if (isDarkTheme) Color.White else MZTheme.DarkText
                        )
                        Box(
                            modifier = Modifier
                                .rotate(-2f)
                                .background(MZTheme.AcidMint.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp))
                                .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.2f) else MZTheme.AcidMint, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "PNU 캠퍼스",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isDarkTheme) Color.White else MZTheme.DarkText
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .background(
                                color = if (isDarkTheme) Color(0x33FFFFFF) else Color(0xCCFFFFFF),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .border(1.dp, if (isDarkTheme) Color(0x1FEEEEEE) else Color.White, androidx.compose.foundation.shape.CircleShape)
                            .clickable { isDarkTheme = !isDarkTheme },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "테마 변환",
                            tint = if (isDarkTheme) Color.White else MZTheme.DarkSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Big Target Group Selection Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(isDarkTheme, cornerRadius = 24)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "👥 업로드할 과방 채널 선택 target group",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isDarkTheme) MZTheme.AcidMint else MZTheme.DarkSlate
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "감정 점수가 선택한 소그룹 대학 피드에 업로드 및 랭킹에 등록됩니다.",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) MZTheme.MutedText else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupsList.forEach { grp ->
                            val isSelected = grp.groupId == selectedGroupId
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) MZTheme.DarkSlate else (if (isDarkTheme) Color(0x1AFFFFFF) else Color(0x12000000)),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MZTheme.AcidMint else Color.Transparent,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        selectedGroupId = grp.groupId
                                        FirebaseRemoteMock.activeGroupId = grp.groupId
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = grp.name.take(9) + if (grp.name.length > 9) ".." else "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else (if (isDarkTheme) Color.LightGray else MZTheme.DarkText)
                                )
                            }
                        }
                    }
                }
            }

            // Big Daily Mission Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(isDarkTheme, cornerRadius = 28)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TODAY'S COMBO",
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (isDarkTheme) MZTheme.AcidMint else MZTheme.DarkSlate,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isTodayCompleted) MZTheme.AcidMint.copy(alpha = 0.25f) else MZTheme.BubblePink.copy(alpha = 0.25f),
                                    RoundedCornerShape(20.dp)
                                )
                                .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.2f) else MZTheme.DarkSlate.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isTodayCompleted) "참여 완료 (추가 제출 가능)" else "미션 미완료 🎯",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isDarkTheme) Color.White else MZTheme.DarkText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mission title representation
                    Text(
                        text = todayTarget.entries.joinToString(separator = " + ") { (emo, ratio) ->
                            val koreanEmo = when (emo) {
                                "HAPPY" -> "기쁨"
                                "SAD" -> "슬픔"
                                "ANGRY" -> "분노"
                                "SURPRISED" -> "놀람"
                                else -> emo
                            }
                            "$koreanEmo ${ (ratio * 10).toInt() / 10f }"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDarkTheme) Color.White else MZTheme.DarkSlate
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (aiTagline != null) "🤖 AI 한 줄" else "오늘의 한 줄",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) MZTheme.AcidMint else MZTheme.DarkSlate
                        )
                    }
                    Text(
                        text = "\"${aiTagline ?: fallbackTagline}\"",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) MZTheme.MutedText else Color.Gray,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onStartMission(targetJson, selectedGroupId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_mission_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MZTheme.BubblePink,
                            contentColor = MZTheme.DarkText
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = MZTheme.DarkText)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "내 감정 표현하러 가기 (셀카 촬영 📸)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MZTheme.DarkText
                        )
                    }
                }
            }

            // Quick Menu Items
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Group Feed button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .glassCard(isDarkTheme, cornerRadius = 24)
                            .clickable { onOpenFeed(FirebaseRemoteMock.activeGroupId) }
                            .padding(12.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(MZTheme.GlassPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) MZTheme.AcidMint else MZTheme.GlassPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "대학 피드 🗣️",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDarkTheme) Color.White else MZTheme.DarkText
                            )
                            Text(
                                text = "동기들 점수 & 순위 구경",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) MZTheme.MutedText else Color.Gray
                            )
                        }
                    }

                    // Archive button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .glassCard(isDarkTheme, cornerRadius = 24)
                            .clickable { onOpenArchive(FirebaseRemoteMock.activeGroupId) }
                            .padding(12.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(MZTheme.GlassSecondary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AllInbox,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) MZTheme.BubblePink else MZTheme.GlassSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "아카이브 🎞️",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDarkTheme) Color.White else MZTheme.DarkText
                            )
                            Text(
                                text = "지나간 셀카 자동 슬라이드쇼",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) MZTheme.MutedText else Color.Gray
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// Helper to make target emotion text readable
fun getTargetReadable(json: String): String {
    return try {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Float::class.javaObjectType)
        val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter<Map<String, Float>>(type)
        val map = adapter.fromJson(json) ?: return "정보 없음"
        map.entries.joinToString(" + ") { (emo, ratio) ->
            val kor = when (emo) {
                "HAPPY" -> "기쁨"
                "SAD" -> "슬픔"
                "ANGRY" -> "분노"
                "SURPRISED" -> "놀람"
                else -> emo
            }
            "$kor ${ (ratio * 10).toInt() / 10f }"
        }
    } catch (e: Exception) {
        "혼합 감정"
    }
}
