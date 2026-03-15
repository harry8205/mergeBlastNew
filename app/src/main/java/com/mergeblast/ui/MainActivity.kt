package com.mergeblast.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mergeblast.MergeBlastApp
import com.mergeblast.ui.game.GameScreen
import com.mergeblast.ui.tutorial.TutorialScreen
import com.mergeblast.ui.leaderboard.LeaderboardScreen
import com.mergeblast.ui.menu.MenuScreen
import com.mergeblast.ui.quest.QuestScreen
import com.mergeblast.ui.shop.ShopScreen
import com.mergeblast.viewmodel.GameViewModel
import com.mergeblast.viewmodel.GameViewModelFactory

// ─── Splash Activity ──────────────────────────────────────────────────────────
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent { SplashScreen() }
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2200L)
    }
}

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1035), Color(0xFF0D1B3E)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "MERGE", color = Color(0xFF4ECDC4), fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp
            )
            Text(
                "BLAST", color = Color(0xFFFF6B6B), fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp
            )
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(
                color = Color(0xFF4ECDC4).copy(alpha = alpha),
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

// ─── Main Activity ────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels {
        GameViewModelFactory(MergeBlastApp.INSTANCE.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            MaterialTheme {
                MergeBlastNavigation(viewModel = viewModel)
            }
        }
    }
}

// ─── Navigation ───────────────────────────────────────────────────────────────
@Composable
fun MergeBlastNavigation(viewModel: GameViewModel) {
    val navController = rememberNavController()
    val tutorialDone by viewModel.tutorialDone.collectAsState(initial = true)

    // Determine start destination — show tutorial only on first launch
    val startDest = if (!tutorialDone) "tutorial" else "menu"

    NavHost(navController = navController, startDestination = startDest) {

        // ── Tutorial ──────────────────────────────────────────────────────────
        composable(
            route = "tutorial",
            enterTransition = { fadeIn(tween(400)) },
            exitTransition   = { fadeOut(tween(300)) }
        ) {
            TutorialScreen(
                onFinish = {
                    viewModel.completeTutorial()
                    navController.navigate("menu") {
                        popUpTo("tutorial") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "menu",
            enterTransition = { fadeIn(tween(300)) },
            exitTransition   = { fadeOut(tween(200)) }
        ) {
            MenuScreen(
                viewModel    = viewModel,
                onPlay       = { navController.navigate("game") },
                onShop       = { navController.navigate("shop") },
                onQuest      = { navController.navigate("quest") },
                onLeaderboard = { navController.navigate("leaderboard") }
            )
        }

        composable(
            route = "game",
            enterTransition = { slideInVertically(tween(350)) { it } + fadeIn(tween(350)) },
            exitTransition   = { slideOutVertically(tween(250)) { it } + fadeOut(tween(250)) }
        ) {
            GameScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(
            route = "shop",
            enterTransition = { slideInHorizontally(tween(320)) { it } + fadeIn() },
            exitTransition   = { slideOutHorizontally(tween(250)) { it } + fadeOut() }
        ) {
            ShopScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(
            route = "quest",
            enterTransition = { slideInVertically(tween(320)) { -it } + fadeIn() },
            exitTransition   = { slideOutVertically(tween(250)) { -it } + fadeOut() }
        ) {
            QuestScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(
            route = "leaderboard",
            enterTransition = { slideInHorizontally(tween(320)) { -it } + fadeIn() },
            exitTransition   = { slideOutHorizontally(tween(250)) { -it } + fadeOut() }
        ) {
            LeaderboardScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

// ─── Helper ───────────────────────────────────────────────────────────────────
fun ComponentActivity.hideSystemUI() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}