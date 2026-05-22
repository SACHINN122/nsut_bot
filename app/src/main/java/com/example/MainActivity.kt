package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.screens.ChatDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.ScreenState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: AttendanceViewModel = viewModel()
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: AttendanceViewModel) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    val studentProfile by viewModel.studentProfile.collectAsStateWithLifecycle()
    val subjectAttendanceList by viewModel.subjectAttendanceList.collectAsStateWithLifecycle()
    val filteredSubjects by viewModel.filteredSubjects.collectAsStateWithLifecycle()
    val attendanceInsights by viewModel.attendanceInsights.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val availableSemesters by viewModel.availableSemesters.collectAsStateWithLifecycle()

    val captchaBitmap by viewModel.captchaBitmap.collectAsStateWithLifecycle()
    val isCaptchaOcrRunning by viewModel.isCaptchaOcrRunning.collectAsStateWithLifecycle()
    val captchaError by viewModel.captchaError.collectAsStateWithLifecycle()
    val captchaText by viewModel.captchaText.collectAsStateWithLifecycle()

    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val isSyncRunning by viewModel.isSyncRunning.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatBotGenerating by viewModel.isChatBotGenerating.collectAsStateWithLifecycle()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val state = screenState) {
                is ScreenState.Login -> {
                    LoginScreen(
                        captchaBitmap = captchaBitmap,
                        isOcrRunning = isCaptchaOcrRunning,
                        errorMessage = captchaError,
                        captchaText = captchaText,
                        onRefreshCaptcha = { viewModel.generateNewCaptcha() },
                        onSolveOcr = { viewModel.solveCaptchaWithOcr() },
                        onLoginClick = { roll, pass, cap ->
                            viewModel.attemptLogin(roll, pass, cap)
                        }
                    )
                }
                is ScreenState.Sync -> {
                    SyncScreen(
                        rollNo = state.rollNo,
                        logs = syncLogs,
                        progress = syncProgress,
                        isSyncRunning = isSyncRunning
                    )
                }
                is ScreenState.MainDashboard -> {
                    ChatDashboardScreen(
                        profile = studentProfile,
                        subjects = subjectAttendanceList,
                        filteredSubjects = filteredSubjects,
                        insights = attendanceInsights,
                        filters = filters,
                        availableSemesters = availableSemesters,
                        messages = chatMessages,
                        isBotGenerating = isChatBotGenerating,
                        onSendMessage = { msg -> viewModel.handleUserSendMessage(msg) },
                        onRefreshData = { viewModel.forceSyncRefresh() },
                        onLogOut = { viewModel.triggerLogOut() },
                        onFilterSemester = { viewModel.updateFilterSemester(it) },
                        onFilterStatus = { viewModel.updateFilterStatus(it) },
                        onFilterSearch = { viewModel.updateFilterSearch(it) },
                        onResetFilters = { viewModel.resetFilters() }
                    )
                }
            }
        }
    }
}
