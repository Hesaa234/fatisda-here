package com.fatisdahere.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> TransisiLayar(
    target: T,
    kedalaman: (T) -> Int,
    modifier: Modifier = Modifier,
    isi: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = {
            val arah = if (kedalaman(targetState) >= kedalaman(initialState)) 1 else -1
            (fadeIn(tween(260, delayMillis = 70)) +
                    slideInHorizontally(tween<IntOffset>(320)) { it / 10 * arah }) togetherWith
                    (fadeOut(tween(140)) +
                            slideOutHorizontally(tween<IntOffset>(320)) { -it / 10 * arah })
        },
        label = "transisiLayar"
    ) { isi(it) }
}

fun Modifier.muncul(): Modifier = composed {
    val sudah = rememberSaveable { mutableStateOf(false) }
    val progres = remember { Animatable(if (sudah.value) 1f else 0f) }
    var posisiY by remember { mutableStateOf<Float?>(null) }
    val kerapatan = LocalDensity.current.density

    LaunchedEffect(posisiY) {
        val y = posisiY
        if (y != null && !sudah.value) {
            delay(((y / kerapatan) * 0.45f).toLong().coerceIn(0L, 240L))
            progres.animateTo(1f, tween(380))
            sudah.value = true
        }
    }

    this
        .onGloballyPositioned { if (posisiY == null) posisiY = it.positionInWindow().y }
        .graphicsLayer {
            alpha = progres.value
            translationY = (1f - progres.value) * 22.dp.toPx()
        }
}
