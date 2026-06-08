package id.ac.pnm.yourtexttoanonymous.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.ac.pnm.yourtexttoanonymous.R

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
val LuxoraGroteskFamily = FontFamily(
    Font(R.font.luxoragrotesk_book, FontWeight.Normal),
    Font(R.font.luxoragrotesk_medium, FontWeight.Bold),
    Font(R.font.luxoragrotesk_italic, FontWeight.Thin),
    Font(R.font.luxoragrotesk_italic, FontWeight.Light)
)