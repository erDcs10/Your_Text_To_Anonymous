package id.ac.pnm.yourtexttoanonymous.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.pnm.yourtexttoanonymous.R
import id.ac.pnm.yourtexttoanonymous.ui.theme.*
import androidx.compose.foundation.Image
@Composable
fun LoginScreen(onLoginClick: () -> Unit) {

    // Pulsing animation for the logo circle
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {

            Spacer(modifier = Modifier.height(64.dp))

            // --- Logo ---
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .scale(pulse)
                    .size(180.dp)
            )



            Spacer(modifier = Modifier.height(40.dp))

            // --- App name ---
            Text(
                text = "YourText",
                fontSize = 32.sp,
                fontFamily = LuxoraGroteskFamily,
                fontWeight = FontWeight.Bold,
                color = TitleColor,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "TO ANONYMOUS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = SubtitleColor,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Chat freely. Stay hidden.",
                fontSize = 14.sp,
                color = SubtitleColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(56.dp))

            // --- Google Sign-In Button ---
            OutlinedButton(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CardBg,
                    contentColor = TitleColor
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                // Google "G" icon circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GoogleBlue.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "G",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoogleBlue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Continue with Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFC5CAE9)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Privacy hint ---
            Text(
                text = "Your identity stays anonymous until you choose to reveal it",
                fontSize = 12.sp,
                color = HintColor,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}