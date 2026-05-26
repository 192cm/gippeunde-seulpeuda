package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.data.*
import com.example.ml.FaceAndEmotionAnalyzer
import com.example.ui.theme.MZTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.glassBackground
import com.example.ui.theme.glassCard
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ResultActivity : ComponentActivity() {

    private lateinit var photoPath: String
    private lateinit var emotionResultJson: String
    private lateinit var emotionTargetJson: String
    private var score: Float = 0f

    private lateinit var database: AppDatabase
    private lateinit var repository: MissionRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        photoPath = intent.getStringExtra("photo") ?: ""
        emotionResultJson = intent.getStringExtra("emotionResult") ?: "{}"
        emotionTargetJson = intent.getStringExtra("emotionTarget") ?: "{}"
        score = intent.getFloatExtra("score", 0f)

        val targetGroupId = intent.getStringExtra("groupId") ?: FirebaseRemoteMock.activeGroupId

        database = AppDatabase.getDatabase(applicationContext)
        repository = MissionRepository(database.missionDao())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            MyApplicationTheme {
                ScoreAndUploadScreen(
                    photoPath = photoPath,
                    emotionResultJson = emotionResultJson,
                    emotionTargetJson = emotionTargetJson,
                    score = score,
                    onUploadClicked = { finalLat, finalLon, locationAddress ->
                        // Save to Local Room DB (MissionRecord)
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val newRecord = MissionRecord(
                            date = todayStr,
                            photoPath = photoPath,
                            targetEmotion = emotionTargetJson,
                            resultEmotion = emotionResultJson,
                            score = score,
                            latitude = finalLat,
                            longitude = finalLon
                        )

                        CoroutineScope(Dispatchers.IO).launch {
                            repository.insert(newRecord)
                            
                            // Parse JSON maps to save into mock Firebase Feed
                            val type = Types.newParameterizedType(Map::class.java, String::class.java, Float::class.javaObjectType)
                            val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter<Map<String, Float>>(type)
                            val targetMap = adapter.fromJson(emotionTargetJson) ?: emptyMap()
                            val resultMap = adapter.fromJson(emotionResultJson) ?: emptyMap()

                            val newSharedFeed = FeedMock(
                                feedId = UUID.randomUUID().toString(),
                                userId = "user_me",
                                userName = "나_부산대컴공_우등생🏅",
                                userProfileEmoji = "💻",
                                groupId = targetGroupId,
                                date = todayStr,
                                photoUrl = photoPath,
                                targetEmotion = targetMap,
                                resultEmotion = resultMap,
                                score = score,
                                latitude = finalLat,
                                longitude = finalLon,
                                address = locationAddress,
                                timestamp = System.currentTimeMillis()
                            )

                            FirebaseRemoteMock.addFeed(applicationContext, newSharedFeed)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(applicationContext, "🎉 로컬 DB 저장 및 부산대 단톡 피드 전송이 완료되었습니다!", Toast.LENGTH_LONG).show()
                                val returnIntent = Intent(applicationContext, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(returnIntent)
                                finish()
                            }
                        }
                    },
                    onCancel = {
                        val returnIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(returnIntent)
                        finish()
                    }
                )
            }
        }
    }
}

data class PnuHotspot(
    val name: String,
    val lat: Double,
    val lon: Double,
    val emoji: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreAndUploadScreen(
    photoPath: String,
    emotionResultJson: String,
    emotionTargetJson: String,
    score: Float,
    onUploadClicked: (Double, Double, String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember {
        if (photoPath.isNotEmpty()) {
            val file = File(photoPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } else null
    }

    // Parse structures
    val targetMap = remember {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Float::class.javaObjectType)
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter<Map<String, Float>>(type).fromJson(emotionTargetJson) ?: emptyMap()
    }

    val resultMap = remember {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Float::class.javaObjectType)
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter<Map<String, Float>>(type).fromJson(emotionResultJson) ?: emptyMap()
    }

    // PNU Locations for easy selection
    val hotspots = remember {
        listOf(
            PnuHotspot("정문 토스트 가게 앞 🥪", 35.2302, 129.0792, "🥪"),
            PnuHotspot("정보컴퓨터공학관 무지개관 🌈", 35.2312, 129.0831, "💻"),
            PnuHotspot("넉넉한터 대운동장 잔디밭 ⚽", 35.2319, 129.0805, "🏟️"),
            PnuHotspot("부산대 건설관 죽음의 고개 🏔️", 35.2335, 129.0779, "⛰️"),
            PnuHotspot("새벽삼거리 연못 술가게 🍻", 35.2329, 129.0822, "🍺")
        )
    }

    var selectedHotspot by remember { mutableStateOf(hotspots[1]) }
    var userLat by remember { mutableStateOf(hotspots[1].lat) }
    var userLon by remember { mutableStateOf(hotspots[1].lon) }
    var customAddress by remember { mutableStateOf(hotspots[1].name) }

    val feedbackMessage = remember(score) {
        when {
            score >= 95 -> "미친 동화율!! 부산대 인간 복사기 인정합니다 👍"
            score >= 85 -> "감정 연출 완벽! 하정우 뺨 때리는 수준의 표정 연기입니다 💯"
            score >= 70 -> "오우 쪼금 비슷하네예. 감정이 축축하이 잘 묻어납니다 🥰"
            score >= 50 -> "아아... 감정이 쪼금 영혼이 없네예. 가식 100% 미소? 😂"
            else -> "이건 뭐 포커페이스입니까? 표정 연습 좀 하고 오이소! 💩"
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .glassBackground(false),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "표정 분석 평가", 
                        fontWeight = FontWeight.Bold,
                        color = MZTheme.DarkText
                    ) 
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Big Score Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 24)
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "최종 분석 스코어",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MZTheme.MutedText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.1f점".format(score),
                            fontWeight = FontWeight.Black,
                            fontSize = 44.sp,
                            color = MZTheme.DarkText,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .background(MZTheme.BubblePink.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = feedbackMessage,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MZTheme.DarkText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Photo Preview & Emotions List comparison side-by-side or stacked
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Image thumbnail
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .height(190.dp)
                            .glassCard(cornerRadius = 24)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "얼굴 스틸컷",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("프리셋 셀카", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        // Emoji sticker badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(MZTheme.AcidMint.copy(alpha = 0.85f), CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                                .size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🔥", fontSize = 14.sp)
                        }
                    }

                    // Comparison indicators
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .height(190.dp)
                            .glassCard(cornerRadius = 24)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📐 감정 벡터 비교",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = MZTheme.DarkText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        // Show list of target weights
                        val emotions = listOf("HAPPY", "SAD", "ANGRY", "SURPRISED", "NEUTRAL", "FEAR")
                        for (emo in emotions) {
                            val targetVal = targetMap[emo] ?: 0f
                            val resultVal = resultMap[emo] ?: 0f
                            
                            val emoKor = when (emo) {
                                "HAPPY" -> "기쁨"
                                "SAD" -> "슬픔"
                                "ANGRY" -> "분노"
                                "SURPRISED" -> "놀람"
                                "NEUTRAL" -> "무표정"
                                "FEAR" -> "공포"
                                else -> emo
                            }

                            if (targetVal > 0f || resultVal > 0.05f) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = emoKor, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MZTheme.DarkText)
                                        Text(
                                            text = "${(resultVal*100).toInt()}%",
                                            fontSize = 9.sp,
                                            color = MZTheme.MutedText
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { resultVal },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = MZTheme.NeonBlue,
                                        trackColor = Color.LightGray.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Google Maps location selection simulator
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 24)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "📍 Google Map 촬영 위치 태그 첨부",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MZTheme.DarkText
                    )
                    Text(
                        text = "부산대학교 학내 어느 핫스팟에서 셀카를 남기셨나요?",
                        fontSize = 11.sp,
                        color = MZTheme.MutedText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated Google Map View (Gen-Z Grid with cute map design)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(MZTheme.AcidMint.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing custom grids to look like a Map layout
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row {
                                Text("Maps 정상 연동 중", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MZTheme.MutedText)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MZTheme.SunnyYellow, CircleShape)
                                        .size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = selectedHotspot.emoji, fontSize = 18.sp)
                                }
                                Column {
                                    Text(text = selectedHotspot.name, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    Text(
                                        text = "위도: ${selectedHotspot.lat} | 경도: ${selectedHotspot.lon}",
                                        fontSize = 10.sp,
                                        color = MZTheme.MutedText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons to change location hotpots
                    Text(text = "핫스팟 빠른 좌표 선택:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MZTheme.MutedText)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        hotspots.take(3).forEach { spot ->
                            val isSelected = selectedHotspot == spot
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) MZTheme.DarkSlate else Color(0x33FFFFFF),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .border(1.dp, if (isSelected) MZTheme.DarkSlate else Color(0x4DFFFFFF), RoundedCornerShape(20.dp))
                                    .clickable {
                                        selectedHotspot = spot
                                        userLat = spot.lat
                                        userLon = spot.lon
                                        customAddress = spot.name
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = spot.name.split(" ")[0], // just local name
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MZTheme.DarkText
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        hotspots.drop(3).forEach { spot ->
                            val isSelected = selectedHotspot == spot
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) MZTheme.DarkSlate else Color(0x33FFFFFF),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .border(1.dp, if (isSelected) MZTheme.DarkSlate else Color(0x4DFFFFFF), RoundedCornerShape(20.dp))
                                    .clickable {
                                        selectedHotspot = spot
                                        userLat = spot.lat
                                        userLon = spot.lon
                                        customAddress = spot.name
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = spot.name.split(" ")[0],
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MZTheme.DarkText
                                )
                            }
                        }
                    }
                }
            }

            // Standard final action buttons
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MZTheme.BubblePink, contentColor = MZTheme.DarkText),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                    ) {
                        Text("취소 (다시 촬영)", fontWeight = FontWeight.Bold, color = MZTheme.DarkText)
                    }

                    Button(
                        onClick = {
                            onUploadClicked(userLat, userLon, customAddress)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(52.dp)
                            .testTag("save_and_upload_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MZTheme.GlassPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(26.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("피드 올리기 & 저장", fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }
}
