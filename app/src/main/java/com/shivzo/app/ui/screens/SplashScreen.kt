package com.shivzo.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shivzo.app.R
import com.shivzo.app.ui.theme.ShivzoNavyBrand
import com.shivzo.app.ui.theme.ShivzoOrangeBrand
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(40f) }

    // Pulse animation for background ambient glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        // Smooth entrance animations
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700)
        )
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        
        delay(1600)
        onSplashTimeout()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF001A38), // Rich Deep Blue
                        Color(0xFF00305E), // Royal Shivzo Blue
                        Color(0xFF001229)  // Dark Midnight Blue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative Ambient Background Glow Circles
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(pulseScale)
                .alpha(0.18f)
                .background(ShivzoOrangeBrand, shape = CircleShape)
        )

        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(pulseScale * 0.9f)
                .alpha(0.12f)
                .background(Color(0xFF0066CC), shape = CircleShape)
        )

        // Main Splash Content Container
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Logo Card Container with soft elevation & white backdrop for brand highlight
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .fillMaxWidth(0.88f)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = ShivzoOrangeBrand.copy(alpha = 0.4f),
                        ambientColor = Color.Black.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                ShivzoOrangeBrand.copy(alpha = 0.6f),
                                Color.White,
                                ShivzoOrangeBrand.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.shivzo_splash_logo_1785584351308),
                    contentDescription = "Shivzo Brand Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tagline Pill Badge
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.value.toInt()) }
                    .alpha(alpha.value)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = ShivzoOrangeBrand.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ShivzoOrangeBrand, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Fast. Fresh. Yours.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Loading Indicator
            Box(
                modifier = Modifier
                    .alpha(alpha.value)
                    .size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = ShivzoOrangeBrand,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Brand Footer
            Text(
                text = "SHIVZO • EXPRESS DELIVERY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .alpha(alpha.value)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

