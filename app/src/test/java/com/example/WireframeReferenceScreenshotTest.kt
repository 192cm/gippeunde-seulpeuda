package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.MissionRepository
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class WireframeReferenceScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun mainHub_reference() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = MissionRepository(AppDatabase.getDatabase(context).missionDao())

        composeTestRule.setContent {
            MyApplicationTheme {
                MainHubScreen(
                    repository = repository,
                    onStartMission = { _, _ -> },
                    onOpenFeed = {},
                    onOpenArchive = {},
                    generateMissionTagline = { null },
                )
            }
        }

        capture("main_hub")
    }

    @Test
    fun missionCapture_reference() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CameraAndAnalysisScreen(
                    targetMap = mapOf("HAPPY" to 0.4f, "SAD" to 0.35f, "SURPRISED" to 0.25f),
                    rawTargetJson = """{"HAPPY":0.4,"SAD":0.35,"SURPRISED":0.25}""",
                    onStartAnalysis = { _, _, _ -> },
                    onBack = {},
                )
            }
        }

        capture("mission_capture")
    }

    @Test
    fun missionPresetSelected_reference() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CameraAndAnalysisScreen(
                    targetMap = mapOf("HAPPY" to 0.4f, "SAD" to 0.35f, "SURPRISED" to 0.25f),
                    rawTargetJson = """{"HAPPY":0.4,"SAD":0.35,"SURPRISED":0.25}""",
                    onStartAnalysis = { _, _, _ -> },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("정문_토스트 단골").performClick()
        capture("mission_preset_selected")
    }

    @Test
    fun resultUpload_reference() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ScoreAndUploadScreen(
                    photoPath = "",
                    emotionResultJson = """{"HAPPY":0.32,"SAD":0.41,"ANGRY":0.15,"SURPRISED":0.12}""",
                    emotionTargetJson = """{"HAPPY":0.4,"SAD":0.35,"SURPRISED":0.25}""",
                    score = 86.4f,
                    onUploadClicked = { _, _, _ -> },
                    onCancel = {},
                    reverseGeocode = { _, _ -> "부산광역시 금정구 부산대학교 정보컴퓨터공학관" },
                    mapContent = { lat, lon, label, address ->
                        MapPlaceholder(lat, lon, label, address)
                    },
                )
            }
        }

        capture("result_upload")
    }

    @Test
    fun groupFeed_reference() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalWireframeCapturePlaceholders provides true) {
                    GroupFeedAndRankScreen(
                        initialGroupId = "PNUCS1",
                        onBack = {},
                        onOpenArchive = { _, _ -> },
                    )
                }
            }
        }

        capture("group_feed")
    }

    @Test
    fun groupRank_reference() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalWireframeCapturePlaceholders provides true) {
                    GroupFeedAndRankScreen(
                        initialGroupId = "PNUCS1",
                        onBack = {},
                        onOpenArchive = { _, _ -> },
                        initialSelectedTab = 1,
                    )
                }
            }
        }

        capture("group_rank")
    }

    @Test
    fun groupJoinDialog_reference() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalWireframeCapturePlaceholders provides true) {
                    Box(Modifier.fillMaxSize()) {
                        GroupFeedAndRankScreen(
                            initialGroupId = "PNUCS1",
                            onBack = {},
                            onOpenArchive = { _, _ -> },
                        )
                        DialogPlaceholder(
                            title = "6자리 초대코드로 과방 참여",
                            body = "부산대학교 학생 동아리나 스터디방 동기들이 보낸 초대코드를 입력합니다.",
                            field = "초대코드 (예: PNUART)",
                            action = "채널 입장받기",
                        )
                    }
                }
            }
        }

        capture("group_join_dialog")
    }

    @Test
    fun groupCreateDialog_reference() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalWireframeCapturePlaceholders provides true) {
                    Box(Modifier.fillMaxSize()) {
                        GroupFeedAndRankScreen(
                            initialGroupId = "PNUCS1",
                            onBack = {},
                            onOpenArchive = { _, _ -> },
                        )
                        DialogPlaceholder(
                            title = "새로운 감정 과방 개설하기",
                            body = "우리 과, 동아리, 스터디방 단위의 표정 배틀 채널을 개설합니다.",
                            field = "배틀방 이름 / 입장코드",
                            action = "배틀 전방 개설",
                        )
                    }
                }
            }
        }

        capture("group_create_dialog")
    }

    @Test
    fun archive_reference() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalWireframeCapturePlaceholders provides true) {
                    MemoriesSlideshowScreen(
                        groupId = "PNUCS1",
                        selectedDate = "",
                        onBack = {},
                    )
                }
            }
        }

        capture("archive")
    }

    private fun capture(name: String) {
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/wireframe-reference/$name.png"
        )
    }

    @androidx.compose.runtime.Composable
    private fun MapPlaceholder(
        lat: Double,
        lon: Double,
        label: String,
        address: String?
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE7EEF1))
                .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("지도 영역", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                Text(label, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                Text(address ?: "주소 확인 대기", fontSize = 11.sp, color = Color(0xFF64748B))
                Text("%.4f, %.4f".format(lat, lon), fontSize = 10.sp, color = Color(0xFF64748B))
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun DialogPlaceholder(
        title: String,
        body: String,
        field: String,
        action: String
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(28.dp))
                    .padding(22.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF0F172A))
                Text(body, modifier = Modifier.padding(top = 10.dp), fontSize = 13.sp, color = Color(0xFF64748B))
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(field, color = Color(0xFF64748B), fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(action, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
