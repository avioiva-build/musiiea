package com.avioiva.player.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import com.avioiva.player.R

/**
 * AVIOIVA brand identity used across the app.
 *
 * The wordmark mimics the styling of the brand icon: a clean geometric
 * sans with wide letter spacing and the signature teal-cyan-violet glow.
 */

private val BrandGradientColors
    @Composable get() = listOf(
        Color(0xFF2DD4BF), // teal
        Color(0xFF38BDF8), // cyan
        Color(0xFFA78BFA), // violet
    )

/** "AVIOIVA" gradient wordmark, optionally with the small PLAYER subtitle. */
@Composable
fun AvioivaWordmark(showSubtitle: Boolean = true, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "AVIOIVA",
            style = TextStyle(
                brush = Brush.horizontalGradient(BrandGradientColors),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
            ),
        )
        if (showSubtitle) {
            Text(
                text = "P L A Y E R",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 3.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

/** Circular AVIOIVA logo badge for top bars - opens the About page. */
@Composable
fun LogoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.avioiva_logo),
            contentDescription = "About AVIOIVA",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
        )
    }
}
