package com.avioiva.player.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.avioiva.player.R

/**
 * Brand identities used across the app.
 *
 * MUSIIEA is the music player app; AVIOIVA is the independent brand
 * behind it. Both wordmarks mimic the styling of the brand icons: a clean
 * geometric sans with wide letter spacing and the signature
 * orange -> pink -> violet -> blue gradient taken from the MUSIIEA logo.
 */

private val MusiieaGradientColors
    @Composable get() = listOf(
        Color(0xFFFF8A00), // orange
        Color(0xFFF0338D), // hot pink
        Color(0xFF8B5CF6), // violet
        Color(0xFF38BDF8), // sky blue (the note)
    )

/** "MUSIIEA" gradient wordmark - the app identity shown in the top bars. */
@Composable
fun MusiieaWordmark(showSubtitle: Boolean = true, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "MUSIIEA",
            style = TextStyle(
                brush = Brush.horizontalGradient(MusiieaGradientColors),
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

/** "AVIOIVA" gradient wordmark - the developer brand, used on the About page. */
@Composable
fun AvioivaWordmark(modifier: Modifier = Modifier) {
    Text(
        text = "AVIOIVA",
        style = TextStyle(
            brush = Brush.horizontalGradient(MusiieaGradientColors),
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp,
        ),
        modifier = modifier,
    )
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
