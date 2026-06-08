package id.ac.pnm.yourtexttoanonymous.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
    Text(text = "Status: Not Authenticated")
    Button(onClick = onLoginClick) {
        Text("Trigger Google Login")
    }
}