package com.morseglyph.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.theme.Black
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingDim
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.ui.theme.RobotoMono
import kotlinx.coroutines.launch

private data class OnboardingPage(val title: String, val body: String, val symbol: String)

private val pages = listOf(
    OnboardingPage(
        title = "MORSEGLYPH",
        body = "Transmit any message in Morse code using the Nothing Phone Glyph Matrix — light and sound, in sync.",
        symbol = "· — · ·"
    ),
    OnboardingPage(
        title = "HOW IT WORKS",
        body = "Type a message, set your WPM speed, pick an indicator mode, then press TRANSMIT. The Glyph lights up with each dot and dash.",
        symbol = "— · · ·"
    ),
    OnboardingPage(
        title = "GLYPH MATRIX",
        body = "MorseGlyph uses the Nothing GlyphMatrix SDK. Make sure Glyph is enabled in Nothing Settings before transmitting.",
        symbol = "· · · —"
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = page.symbol,
                    color = NothingAccent,
                    fontFamily = RobotoMono,
                    fontSize = 28.sp,
                    letterSpacing = 6.sp
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = page.title,
                    color = NothingAccent,
                    fontFamily = RobotoMono,
                    fontSize = 14.sp,
                    letterSpacing = 4.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = page.body,
                    color = NothingDim,
                    fontFamily = RobotoMono,
                    fontSize = 13.sp,
                    lineHeight = 22.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == pagerState.currentPage) 20.dp else 8.dp, 8.dp)
                            .clip(if (i == pagerState.currentPage) RoundedCornerShape(4.dp) else CircleShape)
                            .background(if (i == pagerState.currentPage) NothingAccent else NothingBorder)
                    )
                }
            }

            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingAccent,
                    contentColor = NothingSurface
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    text = if (isLastPage) "GET STARTED" else "NEXT",
                    fontFamily = RobotoMono,
                    fontSize = 13.sp,
                    letterSpacing = 3.sp
                )
            }
        }
    }
}
