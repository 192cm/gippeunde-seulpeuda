package com.example

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
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
import com.example.data.FeedMock
import com.example.data.FirebaseRemoteMock
import com.example.ui.theme.MZTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.glassBackground
import com.example.ui.theme.glassCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

class ArchiveActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val groupId = intent.getStringExtra("groupId") ?: FirebaseRemoteMock.activeGroupId
        val dateString = intent.getStringExtra("date") ?: ""

        setContent {
            MyApplicationTheme {
                MemoriesSlideshowScreen(
                    groupId = groupId,
                    selectedDate = dateString,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesSlideshowScreen(
    groupId: String,
    selectedDate: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Fetch all public feeds from the active group to make a rich slide-show deck
    val groupFeeds = remember(groupId) {
        FirebaseRemoteMock.getFeeds(context).filter { it.groupId == groupId }
    }

    var currentSlideIndex by remember { mutableStateOf(0) }
    var slideShowIntervalMs by remember { mutableStateOf(2000f) } // default 2 seconds (2000ms)
    var isAutoPlayEnabled by remember { mutableStateOf(true) }

    // Coroutine to drive the slide-show auto progression
    LaunchedEffect(isAutoPlayEnabled, slideShowIntervalMs, groupFeeds.size) {
        if (isAutoPlayEnabled && groupFeeds.isNotEmpty()) {
            while (isActive) {
                delay(slideShowIntervalMs.toLong())
                currentSlideIndex = (currentSlideIndex + 1) % groupFeeds.size
            }
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
                        text = "추억 슬라이드쇼", 
                        fontWeight = FontWeight.Bold,
                        color = MZTheme.DarkText
                    ) 
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 6.dp)
                            .size(36.dp)
                            .background(Color(0xCCFFFFFF), shape = androidx.compose.foundation.shape.CircleShape)
                            .border(1.dp, Color.White, shape = androidx.compose.foundation.shape.CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack, 
                            contentDescription = "홈으로", 
                            tint = MZTheme.DarkSlate,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Subtitle
            Text(
                text = "🎞️ 동기들이 남긴 감정 연기 복기",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MZTheme.MutedText,
                modifier = Modifier.align(Alignment.Start)
            )

            // Slideshow Core Frame with full card layout
            if (groupFeeds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .glassCard(cornerRadius = 24),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "아카이브할 사진 기록이 없습니다.",
                        fontWeight = FontWeight.Bold,
                        color = MZTheme.MutedText
                    )
                }
            } else {
                val currentFeed = groupFeeds[currentSlideIndex]

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 24)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    // Slide header representing the actor of the selfie
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MZTheme.BubblePink.copy(alpha = 0.25f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(MZTheme.AcidMint.copy(alpha = 0.25f), CircleShape)
                                        .size(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = currentFeed.userProfileEmoji, fontSize = 14.sp)
                                }
                                Column {
                                    Text(text = currentFeed.userName, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    Text(text = currentFeed.date, fontSize =  9.sp, color = MZTheme.MutedText)
                                }
                            }

                            // Score tag on slide
                            Box(
                                modifier = Modifier
                                    .background(MZTheme.BubblePink.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "일치도 ${currentFeed.score.toInt()}%",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = MZTheme.DarkText
                                )
                            }
                        }
                    }

                    // Main image frame of active index slide
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        val file = File(currentFeed.photoUrl)
                        if (file.exists()) {
                            val bmp = BitmapFactory.decodeFile(file.absolutePath)
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                SlideFallbackView(currentFeed)
                            }
                        } else {
                            SlideFallbackView(currentFeed)
                        }

                        // Play state label
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(MZTheme.DarkSlate.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${currentSlideIndex + 1} / ${groupFeeds.size}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bottom info label detail
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Column {
                            val goalsRaw = remember(currentFeed.targetEmotion) {
                                currentFeed.targetEmotion.entries.joinToString(" + ") { (k, v) -> "$k $v" }
                            }
                            val resultsRaw = remember(currentFeed.resultEmotion) {
                                currentFeed.resultEmotion.entries.filter { it.value > 0.05f }.joinToString(" + ") { (k, v) -> "$k ${ "%.1f".format(v) }" }
                            }

                            Text(
                                text = "🎯 미션 감정: $goalsRaw",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "🎭 실제 표정: $resultsRaw",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MZTheme.DarkText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                Text(
                                    text = "배경음악 심상: \"잔잔한 금정산 처량한 바람소리 시뮬레이터\"",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Slide Control Sliders for interval adjustments
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null)
                        Text(text = "자동 슬라이드쇼 설정", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    
                    // Play / Pause Toggle switcher
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = if (isAutoPlayEnabled) "자동 재생 중" else "멈춤", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = isAutoPlayEnabled,
                            onCheckedChange = { isAutoPlayEnabled = it },
                            modifier = Modifier.testTag("archive_autoplay_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // The Speed transition slider!
                Text(
                    text = "슬라이드 전환 간격: " + "%.1f초".format(slideShowIntervalMs / 1000f),
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                
                Slider(
                    value = slideShowIntervalMs,
                    onValueChange = { slideShowIntervalMs = it },
                    valueRange = 1000f..6000f, // 1 second to 6 seconds interval limits
                    steps = 4, // snaps exactly to 1s, 2s, 3s, 4s, 5s, 6s offsets
                    colors = SliderDefaults.colors(
                        thumbColor = MZTheme.DarkSlate,
                        activeTrackColor = MZTheme.DarkSlate,
                        inactiveTrackColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("archive_speed_slider")
                )

                // Manual Step navigations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (groupFeeds.isNotEmpty()) {
                                currentSlideIndex = (currentSlideIndex - 1 + groupFeeds.size) % groupFeeds.size
                                isAutoPlayEnabled = false // pause
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MZTheme.BubblePink, contentColor = MZTheme.DarkText),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                    ) {
                        Text("이전 장", fontWeight = FontWeight.Bold, color = MZTheme.DarkText)
                    }

                    Button(
                        onClick = {
                            if (groupFeeds.isNotEmpty()) {
                                currentSlideIndex = (currentSlideIndex + 1) % groupFeeds.size
                                isAutoPlayEnabled = false // pause
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MZTheme.BubblePink, contentColor = MZTheme.DarkText),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                    ) {
                        Text("다음 장", fontWeight = FontWeight.Bold, color = MZTheme.DarkText)
                    }
                }
            }
        }
    }
}

@Composable
fun SlideFallbackView(feed: FeedMock) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MZTheme.BubblePink),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🦊", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${feed.userName}의 추억",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = MZTheme.DarkText
            )
            Text(
                text = "인스타 감성 샷 무한 보정 완료",
                fontSize = 11.sp,
                color = MZTheme.DarkText.copy(alpha = 0.6f)
            )
        }
    }
}
