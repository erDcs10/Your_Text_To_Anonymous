package id.ac.pnm.yourtexttoanonymous.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.pnm.yourtexttoanonymous.ui.theme.*

@Composable
fun ProfileSetupScreen(
    inputName: String,
    inputGender: String,
    onNameChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    // We force DarkBg directly to the Scaffold containerColor
    Scaffold(
        containerColor = DarkBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg) // Force-paints it a second time to prevent any container leak
                .padding(innerPadding)
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP CONTENT AREA ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = "Complete Your Profile",
                    color = WhiteBg,
                    fontFamily = LuxoraGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Set up your identity before meeting strangers.",
                    color = WhiteBg.copy(alpha = 0.6f),
                    fontFamily = LuxoraGroteskFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
                )

                Text(
                    text = "Who are you?",
                    color = WhiteBg,
                    fontFamily = LuxoraGroteskFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = inputName,
                    onValueChange = onNameChange,
                    placeholder = {
                        Text(
                            text = "Enter nickname or alias...",
                            color = WhiteBg.copy(alpha = 0.3f),
                            fontFamily = LuxoraGroteskFamily
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(
                        color = WhiteBg,
                        fontFamily = LuxoraGroteskFamily
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhiteBg,
                        unfocusedBorderColor = WhiteBg.copy(alpha = 0.2f),
                        cursorColor = WhiteBg
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Select Gender",
                    color = WhiteBg,
                    fontFamily = LuxoraGroteskFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GenderSelectionCard(
                        label = "Male",
                        selectedColor = GoogleBlue,
                        isSelected = inputGender == "Male",
                        modifier = Modifier.weight(1f),
                        onClick = { onGenderChange("Male") }
                    )

                    GenderSelectionCard(
                        label = "Female",
                        selectedColor = Pink80,
                        isSelected = inputGender == "Female",
                        modifier = Modifier.weight(1f),
                        onClick = { onGenderChange("Female") }
                    )
                }
            }

            // --- BOTTOM PRIMARY ACTION BUTTON ---
            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhiteBg,
                    contentColor = DarkBg
                )
            ) {
                Text(
                    text = "Save Profile & Continue",
                    fontFamily = LuxoraGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun GenderSelectionCard(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) selectedColor else WhiteBg.copy(alpha = 0.1f)
    val backgroundColor = if (isSelected) selectedColor.copy(alpha = 0.05f) else Color.Transparent

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) selectedColor else WhiteBg.copy(alpha = 0.5f),
            fontFamily = LuxoraGroteskFamily,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 15.sp
        )
    }
}

// --- UPDATED FIXED PREVIEW GENERATOR ---

@Preview(
    showBackground = false, // Turned off to prevent Android Studio from injecting its own white canvas background
    showSystemUi = true,
    device = "id:pixel_8" // Locks it strictly onto a modern device frame configuration
)
@Composable
fun ProfileSetupScreenPreview() {
    // Explicitly boxing the preview component inside a hard-colored Box to crush white spaces
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        ProfileSetupScreen(
            inputName = "Anonymous",
            inputGender = "Male",
            onNameChange = {},
            onGenderChange = {},
            onSaveClick = {}
        )
    }
}