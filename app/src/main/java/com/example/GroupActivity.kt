package com.example

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.data.FeedMock
import com.example.data.FirebaseRemoteMock
import com.example.data.GroupMock
import com.example.ui.theme.MZTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.glassBackground
import com.example.ui.theme.glassCard
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GroupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialGroupId = intent.getStringExtra("groupId") ?: FirebaseRemoteMock.activeGroupId

        setContent {
            MyApplicationTheme {
                GroupFeedAndRankScreen(
                    initialGroupId = initialGroupId,
                    onBack = { finish() },
                    onOpenArchive = { groupId, date ->
                        val intent = Intent(this, ArchiveActivity::class.java).apply {
                            putExtra("groupId", groupId)
                            putExtra("date", date)
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
fun GroupFeedAndRankScreen(
    initialGroupId: String,
    onBack: () -> Unit,
    onOpenArchive: (String, String) -> Unit
) {
    val context = LocalContext.current
    var currentGroupId by remember { mutableStateOf(initialGroupId) }
    
    // Remote state managers
    var groupsList by remember { mutableStateOf(FirebaseRemoteMock.getGroups(context)) }
    var feedsList by remember { mutableStateOf(FirebaseRemoteMock.getFeeds(context)) }

    // Join Group input state
    var inviteCodeInput by remember { mutableStateOf("") }
    var joinGroupDialogOpen by remember { mutableStateOf(false) }
    var createGroupDialogOpen by remember { mutableStateOf(false) }

    // Create Group input states
    var createGroupNameInput by remember { mutableStateOf("") }
    var createGroupCodeInput by remember { mutableStateOf("") }

    val activeGroup = remember(currentGroupId, groupsList) {
        groupsList.find { it.groupId == currentGroupId } ?: groupsList.firstOrNull() ?: FirebaseRemoteMock.defaultGroups.first()
    }

    // Leaderboard rankings computed from scores
    val rankingList = remember(activeGroup, feedsList) {
        val filtered = feedsList.filter { it.groupId == activeGroup.groupId }
        filtered.sortedByDescending { it.score }
    }

    // Tab states
    var selectedTabState by remember { mutableStateOf(0) } // 0: Feed Timeline, 1: Live Group Rankings

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .glassBackground(false),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = activeGroup.name, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 20.sp,
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
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xCCFFFFFF), shape = androidx.compose.foundation.shape.CircleShape)
                            .border(1.dp, Color.White, shape = androidx.compose.foundation.shape.CircleShape)
                            .clickable { joinGroupDialogOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd, 
                            contentDescription = "초대코드 참가", 
                            tint = MZTheme.DarkSlate,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .background(Color(0xCCFFFFFF), shape = androidx.compose.foundation.shape.CircleShape)
                            .border(1.dp, Color.White, shape = androidx.compose.foundation.shape.CircleShape)
                            .clickable { createGroupDialogOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddHomeWork, 
                            contentDescription = "새 그룹 방 만들기", 
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
        ) {
            
            // Channel Switcher Horizon list
            Text(
                text = "⚡ 가입된 소규모 채널:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = MZTheme.MutedText
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupsList.forEach { grp ->
                    val isSelected = grp.groupId == activeGroup.groupId
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) MZTheme.DarkSlate else Color(0x66FFFFFF),
                                RoundedCornerShape(20.dp)
                            )
                            .border(
                                1.dp, 
                                if (isSelected) MZTheme.DarkSlate else Color(0xCCFFFFFF), 
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                currentGroupId = grp.groupId
                                FirebaseRemoteMock.activeGroupId = grp.groupId
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = grp.name.take(9) + if (grp.name.length > 9) ".." else "",
                            color = if (isSelected) Color.White else MZTheme.DarkText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub tabs for Feed vs Rank
            TabRow(
                selectedTabIndex = selectedTabState,
                containerColor = Color.Transparent,
                contentColor = MZTheme.DarkSlate
            ) {
                Tab(
                    selected = selectedTabState == 0,
                    onClick = { selectedTabState = 0 },
                    text = { Text("실시간 피드 🗣️", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabState == 1,
                    onClick = { selectedTabState = 1 },
                    text = { Text("미션 라이브 랭킹 🏆", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Group Metadata banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .glassCard(cornerRadius = 16)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔑 초대코드: ${activeGroup.inviteCode}  |  👥 멤버 ${activeGroup.memberIds.size}명",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MZTheme.DarkText
                    )
                    Text(
                        text = "아카이브 보기 🎞️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MZTheme.GlassPrimary,
                        modifier = Modifier.clickable {
                            onOpenArchive(activeGroup.groupId, todayStr)
                        }
                    )
                }
            }

            // Timeline details list
            if (rankingList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Text(
                            text = "아직 이 채널에 오늘 업로드된 셀카가 없심더.\n오늘 첫 번째 주인공이 되어 보이소!",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    if (selectedTabState == 0) {
                        // FEED GRID TIMELINE (Instagram Screenshot Friendly!)
                        // This replaces the scrolled item timeline with a neat, single-screen grid board showing ALL members.
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Calculate group specific target emotion combo
                            val groupTargetMap = remember(activeGroup.groupId, todayStr) {
                                FirebaseRemoteMock.getGroupTargetEmotion(activeGroup.groupId, todayStr)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(Color(0xFF1E293B).copy(alpha = 0.9f), Color(0xFF0F172A).copy(alpha = 0.95f))
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .border(2.dp, MZTheme.AcidMint, RoundedCornerShape(24.dp))
                                    .padding(14.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Header of Board
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "🔥 ${activeGroup.name} 감정 캐치",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "오늘의 목표: " + groupTargetMap.entries.joinToString(" + ") { (emo, ratio) ->
                                                    val kor = when (emo) {
                                                        "HAPPY" -> "기쁨"
                                                        "SAD" -> "슬픔"
                                                        "ANGRY" -> "분노"
                                                        "SURPRISED" -> "놀람"
                                                        "NEUTRAL" -> "무표정"
                                                        "FEAR" -> "공포"
                                                        else -> emo
                                                    }
                                                    "$kor ${(ratio * 10).toInt() / 10f}"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MZTheme.AcidMint
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(MZTheme.BubblePink, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "인스타 자랑용 📸",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 8.sp,
                                                color = MZTheme.DarkText
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(14.dp))
                                    
                                    // 2-Column Grid of members
                                    val memberIds = activeGroup.memberIds
                                    val chunked = memberIds.chunked(2)
                                    
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        chunked.forEach { pairOfMembers ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                pairOfMembers.forEach { memberId ->
                                                    val feedForMember = rankingList.find { it.userId == memberId }
                                                    val (name, emoUnicode) = getMemberProfile(memberId)
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(140.dp)
                                                            .background(
                                                                if (feedForMember != null) Color(0x1F22D3EE) else Color(0x1A6B7280),
                                                                RoundedCornerShape(16.dp)
                                                            )
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (feedForMember != null) MZTheme.AcidMint.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                                                                shape = RoundedCornerShape(16.dp)
                                                            )
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(8.dp),
                                                            verticalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            // Member info header
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Text(text = emoUnicode, fontSize = 14.sp)
                                                                    Text(
                                                                        text = name.split("_").last().take(6),
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color.White
                                                                    )
                                                                }
                                                                
                                                                if (feedForMember != null) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .background(MZTheme.AcidMint, RoundedCornerShape(6.dp))
                                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = "${feedForMember.score.toInt()}pt",
                                                                            fontWeight = FontWeight.Black,
                                                                            fontSize = 9.sp,
                                                                            color = MZTheme.DarkText
                                                                        )
                                                                    }
                                                                } else {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = "미제출 👻",
                                                                            fontWeight = FontWeight.Bold,
                                                                            fontSize = 8.sp,
                                                                            color = Color.Red
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            
                                                            // Visual content (Thumbnail photobox)
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(76.dp)
                                                                    .background(Color(0x33000000), RoundedCornerShape(10.dp))
                                                                    .clip(RoundedCornerShape(10.dp)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (feedForMember != null) {
                                                                    val file = File(feedForMember.photoUrl)
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
                                                                            PlaceholderStickerSm(feedForMember.userName)
                                                                        }
                                                                    } else {
                                                                        PlaceholderStickerSm(feedForMember.userName)
                                                                    }
                                                                } else {
                                                                    Text(
                                                                        text = "기다리는 중..",
                                                                        fontSize = 10.sp,
                                                                        color = Color.LightGray.copy(alpha = 0.6f),
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                            
                                                            // Footer info
                                                            if (feedForMember != null) {
                                                                val actMapStr = feedForMember.resultEmotion.entries
                                                                    .filter { it.value > 0.15f }
                                                                    .joinToString(" ") { (k, v) ->
                                                                        val kKor = when(k) {
                                                                            "HAPPY" -> "기쁨"
                                                                            "SAD" -> "슬픔"
                                                                            "ANGRY" -> "분노"
                                                                            "SURPRISED" -> "놀람"
                                                                            "NEUTRAL" -> "무"
                                                                            "FEAR" -> "공포"
                                                                            else -> k
                                                                        }
                                                                        "$kKor${(v*10).toInt()}"
                                                                    }
                                                                Text(
                                                                    text = actMapStr,
                                                                    fontSize = 9.sp,
                                                                    color = MZTheme.AcidMint,
                                                                    maxLines = 1,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                            } else {
                                                                Text(
                                                                    text = "얼굴을 올려주세요!",
                                                                    fontSize = 9.sp,
                                                                    color = Color.Gray,
                                                                    maxLines = 1
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                // If odd count of entries in pair, fill with Spacer
                                                if (pairOfMembers.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // LIVE RANK TAB
                        item {
                            Text(
                                text = "🏆 오늘의 감정 챔피언 순위",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(rankingList.mapIndexed { idx, item -> idx to item }) { (index, item) ->
                            val crownColor = when (index) {
                                0 -> Color(0xFFFFD700) // Gold
                                1 -> Color(0xFFC0C0C0) // Silver
                                2 -> Color(0xFFCD7F32) // Bronze
                                else -> Color(0x33FFFFFF)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassCard(cornerRadius = 20)
                                    .then(
                                        if (index == 0) Modifier.background(Color(0x1AFFFFE0), RoundedCornerShape(20.dp))
                                        else Modifier
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        // Rank slot
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (index < 3) crownColor else Color(0x1F1E293B),
                                                    CircleShape
                                                )
                                                .border(1.dp, if (index < 3) MZTheme.DarkSlate else Color(0x33FFFFFF), CircleShape)
                                                .size(28.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (index + 1).toString(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (index < 3) MZTheme.DarkText else MZTheme.MutedText
                                            )
                                        }

                                        Text(text = item.userProfileEmoji, fontSize = 20.sp)

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(text = item.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                if (index == 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xCCFFD700), RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("오늘의 1위 👑", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = MZTheme.DarkText)
                                                    }
                                                }
                                            }
                                            Text(text = "분석 오차율 대폭 감쇠 - 일치 점수 ${item.score.toInt()}pt", fontSize = 10.sp, color = MZTheme.MutedText)
                                        }
                                    }

                                    Text(
                                        text = "%.1f점".format(item.score),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = if (index == 0) MZTheme.GlassPrimary else MZTheme.DarkText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Join Group dialog formulation
    if (joinGroupDialogOpen) {
        AlertDialog(
            onDismissRequest = { joinGroupDialogOpen = false },
            title = { Text("6자리 초대코드로 과방 참여", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("부산대학교 학생 동아리나 스터디방 동기들이 보낸 6자리 영문/숫자 초대코드를 입력하세요.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inviteCodeInput,
                        onValueChange = { inviteCodeInput = it.take(10) },
                        label = { Text("초대코드 (예: PNUART)") },
                        modifier = Modifier.fillMaxWidth().testTag("join_group_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inviteCodeInput.trim().length < 3) {
                            Toast.makeText(context, "올바른 초대코드를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val matched = FirebaseRemoteMock.joinGroupByCode(context, inviteCodeInput.trim())
                        if (matched != null) {
                            Toast.makeText(context, "🎉 '${matched.name}' 그룹에 성공적으로 참석했습니다!", Toast.LENGTH_SHORT).show()
                            groupsList = FirebaseRemoteMock.getGroups(context)
                            currentGroupId = matched.groupId
                            FirebaseRemoteMock.activeGroupId = matched.groupId
                            joinGroupDialogOpen = false
                        } else {
                            Toast.makeText(context, "존재하지 않거나 만료된 초대코드입니다. (기본 코드: PNUCS1, PNUART, PNUTRA)", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MZTheme.DarkSlate)
                ) {
                    Text("채널 입장받기")
                }
            },
            dismissButton = {
                TextButton(onClick = { joinGroupDialogOpen = false }) {
                    Text("취소")
                }
            }
        )
    }

    // Create Group Dialog
    if (createGroupDialogOpen) {
        AlertDialog(
            onDismissRequest = { createGroupDialogOpen = false },
            title = { Text("새로운 감정 과방 개설하기", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("우리 과 컴공 단톡방, 미대 친구들, 넉터 술방 등 소규모 표정 배틀 채널을 개설합니다.", fontSize = 12.sp, color = Color.Gray)
                    
                    OutlinedTextField(
                        value = createGroupNameInput,
                        onValueChange = { createGroupNameInput = it },
                        label = { Text("배틀방 이름 (예: 부산대 기공과 낭만단 🛠️)") },
                        modifier = Modifier.fillMaxWidth().testTag("create_group_name_input")
                    )

                    OutlinedTextField(
                        value = createGroupCodeInput,
                        onValueChange = { createGroupCodeInput = it.take(8) },
                        label = { Text("배틀방 입장코드 (6-8자리 영문/숫자)") },
                        modifier = Modifier.fillMaxWidth().testTag("create_group_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (createGroupNameInput.trim().isEmpty() || createGroupCodeInput.trim().length < 3) {
                            Toast.makeText(context, "방 이름과 입장코드를 정밀히 적어주이소.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val newG = FirebaseRemoteMock.addGroup(context, createGroupNameInput.trim(), createGroupCodeInput.trim())
                        Toast.makeText(context, "🎉 '${newG.name}' 개설 완료! 초대코드를 친구들에게 공유해 보이소.", Toast.LENGTH_LONG).show()
                        groupsList = FirebaseRemoteMock.getGroups(context)
                        currentGroupId = newG.groupId
                        FirebaseRemoteMock.activeGroupId = newG.groupId
                        createGroupDialogOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MZTheme.DarkSlate)
                ) {
                    Text("배틀 전방 개설")
                }
            },
            dismissButton = {
                TextButton(onClick = { createGroupDialogOpen = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun PlaceholderSticker(userName: String) {
    val seed = userName.hashCode()
    val random = Random(seed.toLong())
    val colors = listOf(MZTheme.BubblePink, MZTheme.SoftLilac, MZTheme.AcidMint, MZTheme.SunnyYellow, MZTheme.NeonBlue)
    val chosenCol = colors[random.nextInt(colors.size)]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chosenCol),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🦊", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "부산대 최우수 앙상블",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MZTheme.DarkText.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun PlaceholderStickerSm(userName: String) {
    val seed = userName.hashCode()
    val random = Random(seed.toLong())
    val colors = listOf(MZTheme.BubblePink, MZTheme.SoftLilac, MZTheme.AcidMint, MZTheme.SunnyYellow, MZTheme.NeonBlue)
    val chosenCol = colors[random.nextInt(colors.size)]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chosenCol),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "🦊", fontSize = 24.sp)
    }
}

fun getMemberProfile(memberId: String): Pair<String, String> {
    return when (memberId) {
        "user_me" -> "정문_과음(나)" to "💻"
        "user_minji" -> "토스트_민지" to "🦊"
        "user_hyunu" -> "등반_현우" to "🐻"
        "user_jiung" -> "금정산성_지웅" to "🦁"
        "user_yujin" -> "아카데미_유진" to "🐰"
        "user_somin" -> "인스타_소민" to "💅"
        "user_taewoo" -> "대운동장_태우" to "🏃"
        "user_gildong" -> "도서관_길동" to "👴"
        "user_heewon" -> "카페_희원" to "☕"
        else -> "과원_동기" to "🎓"
    }
}
