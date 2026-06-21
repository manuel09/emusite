package com.emusite.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.emusite.app.data.MainViewModel

@Composable
fun PlayerScreen(
    sourceId: String,
    url: String,
    title: String,
    onBack: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var showOverlay by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(sourceId, url) {
        viewModel.loadStreamLinks(sourceId, url)
    }

    LaunchedEffect(state.selectedStream) {
        exoPlayer?.release()
        isReady = false
        showOverlay = false
        isBuffering = true
        val stream = state.selectedStream ?: return@LaunchedEffect

        val player = ExoPlayer.Builder(context).build().also { exoPlayer = it }

        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            setDefaultRequestProperties(stream.headers)
        }
        val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(stream.url)))

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) isReady = true
            }
        })

        player.setMediaSource(hlsSource)
        player.playWhenReady = true
        player.prepare()
    }

    DisposableEffect(Unit) {
        val activity = view.context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        activity?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            WindowCompat.setDecorFitsSystemWindows(this, false)
            WindowCompat.getInsetsController(this, view).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        onDispose {
            exoPlayer?.release()
            activity?.window?.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                WindowCompat.setDecorFitsSystemWindows(this, true)
                WindowCompat.getInsetsController(this, view).apply {
                    show(WindowInsetsCompat.Type.systemBars())
                }
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { showOverlay = !showOverlay }
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                    AndroidKeyEvent.KEYCODE_ENTER,
                    AndroidKeyEvent.KEYCODE_BUTTON_A -> {
                        showOverlay = !showOverlay
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        exoPlayer?.let {
                            if (it.isPlaying) it.pause() else it.play()
                        }
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        exoPlayer?.seekTo((exoPlayer?.currentPosition ?: 0) + 10000)
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> {
                        exoPlayer?.seekTo(((exoPlayer?.currentPosition ?: 0) - 10000).coerceAtLeast(0))
                        true
                    }
                    AndroidKeyEvent.KEYCODE_BACK -> {
                        onBack()
                        true
                    }
                    else -> false
                }
            }
            .focusable()
    ) {
        when {
            state.isLoading -> {
                LoadingOverlay("Preparing stream...")
            }
            state.error != null -> {
                ErrorOverlay(state.error ?: "Error", onBack)
            }
            exoPlayer != null -> {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            controllerAutoShow = false
                            controllerShowTimeoutMs = 0
                            setKeepContentOnPlayerReset(true)
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Buffering overlay
                if (isBuffering && !isReady) {
                    LoadingOverlay("Buffering...")
                }

                // Controls overlay
                AnimatedVisibility(
                    visible = showOverlay,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Top gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(0.85f), Color.Transparent)
                                    )
                                )
                                .align(Alignment.TopCenter)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 40.dp, end = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White.copy(0.9f))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (state.selectedStream != null) {
                                        Text(
                                            state.selectedStream!!.quality,
                                            color = Color.White.copy(0.6f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(0.5f))
                                    )
                                )
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }
            state.streams.isEmpty() && !state.isLoading -> {
                ErrorOverlay("No streams available") {
                    viewModel.loadStreamLinks(sourceId, url)
                }
            }
        }
    }
}

@Composable
private fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color.White.copy(0.7f),
                strokeWidth = 2.dp,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = Color.White.copy(0.5f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun ErrorOverlay(error: String, onAction: (() -> Unit)? = null) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(error, color = Color.White.copy(0.7f), fontSize = 13.sp)
            if (onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f)),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Retry", color = Color.White, fontSize = 13.sp) }
            }
        }
    }
}

private fun tween(duration: Int): androidx.compose.animation.core.TweenSpec<Float> =
    androidx.compose.animation.core.tween(duration)
