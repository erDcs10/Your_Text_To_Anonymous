package id.ac.pnm.yourtexttoanonymous

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.ac.pnm.yourtexttoanonymous.ui.theme.YourTextToAnonymousTheme
import kotlinx.coroutines.launch
import android.util.Log


class MainActivity : ComponentActivity() {

    private val authManager by lazy { AuthManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            YourTextToAnonymousTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 48.dp, start = 16.dp)
                ) {
                    val scope = rememberCoroutineScope()
                    val currentUser = authManager.getCurrentUser()

                    if (currentUser == null) {
                        Text(text = "Status: Not Authenticated")
                        Button(onClick = {
                            scope.launch {
                                val result = authManager.authenticateWithGoogle()
                                if (result.isSuccess) {
                                    Log.d("Auth", "Logged in as: ${result.getOrNull()?.uid}")
                                } else {
                                    Log.e("Auth", "Login error: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }) {
                            Text("Trigger Google Login")
                        }
                    } else {
                        Text(text = "Status: Authenticated")
                        Text(text = "UID: ${currentUser.uid}")
                    }
                }
            }
        }
    }
}
