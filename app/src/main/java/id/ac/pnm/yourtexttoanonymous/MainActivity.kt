package id.ac.pnm.yourtexttoanonymous

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import id.ac.pnm.yourtexttoanonymous.data.local.AppDatabase
import id.ac.pnm.yourtexttoanonymous.data.remote.AuthManager
import id.ac.pnm.yourtexttoanonymous.data.remote.ChatManager
import id.ac.pnm.yourtexttoanonymous.data.remote.MatchmakingManager
import id.ac.pnm.yourtexttoanonymous.data.remote.ProfileManager
import id.ac.pnm.yourtexttoanonymous.navigation.AppNavigation
import id.ac.pnm.yourtexttoanonymous.navigation.Screen
import id.ac.pnm.yourtexttoanonymous.ui.theme.DarkBg
import id.ac.pnm.yourtexttoanonymous.ui.theme.YourTextToAnonymousTheme

class MainActivity : ComponentActivity() {

    private val authManager by lazy { AuthManager(this) }
    private val matchmakingManager = MatchmakingManager()
    private val profileManager = ProfileManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appDb = AppDatabase.getDatabase(this)

        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false  // false = white system bar icons

        enableEdgeToEdge()

        setContent {
            YourTextToAnonymousTheme {
                val currentUser = authManager.getCurrentUser()

                var chatManager by remember { mutableStateOf<ChatManager?>(null) }
                var isProfileComplete by remember { mutableStateOf<Boolean?>(null) }
                var persistentRooms by remember { mutableStateOf<List<String>>(emptyList()) }

                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        profileManager.checkProfileExists(currentUser.uid) { exists ->
                            isProfileComplete = exists
                            if (exists) {
                                val manager = ChatManager(appDb.messageDao(), currentUser.uid)
                                chatManager = manager
                                manager.listenForPersistentRooms { rooms -> persistentRooms = rooms }
                            }
                        }
                    } else {
                        isProfileComplete = false
                        chatManager = null
                        persistentRooms = emptyList()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    when {
                        currentUser != null && isProfileComplete == null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Verifying profile...",
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }

                        else -> {
                            val initialDestination = when {
                                currentUser == null -> Screen.Login.route
                                isProfileComplete == false -> Screen.ProfileSetup.route
                                else -> Screen.Inbox.route
                            }

                            AppNavigation(
                                startDestination = initialDestination,
                                currentUser = currentUser,
                                authManager = authManager,
                                profileManager = profileManager,
                                matchmakingManager = matchmakingManager,
                                appDb = appDb,
                                chatManager = chatManager,
                                persistentRooms = persistentRooms,
                                onUpdateChatManager = { chatManager = it },
                                onUpdatePersistentRooms = { persistentRooms = it },
                                onUpdateProfileComplete = { isProfileComplete = it }
                            )
                        }
                    }
                }
            }
        }
    }
}