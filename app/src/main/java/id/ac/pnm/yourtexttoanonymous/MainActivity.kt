package id.ac.pnm.yourtexttoanonymous

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import id.ac.pnm.yourtexttoanonymous.ui.chat.ChatScreen
import id.ac.pnm.yourtexttoanonymous.ui.chat.InboxScreen
import id.ac.pnm.yourtexttoanonymous.ui.auth.LoginScreen
import id.ac.pnm.yourtexttoanonymous.ui.profile.ProfileSetupScreen
import id.ac.pnm.yourtexttoanonymous.ui.theme.YourTextToAnonymousTheme
import id.ac.pnm.yourtexttoanonymous.ui.theme.DarkBg // Make sure to import your DarkBg color token here
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val authManager by lazy { AuthManager(this) }
    private val matchmakingManager = MatchmakingManager()
    private val profileManager = ProfileManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appDb = AppDatabase.getDatabase(this)

        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false  // false = white icons

        enableEdgeToEdge()

        setContent {
            YourTextToAnonymousTheme {
                val scope = rememberCoroutineScope()
                val currentUser = authManager.getCurrentUser()

                var chatManager by remember { mutableStateOf<ChatManager?>(null) }
                var isProfileComplete by remember { mutableStateOf<Boolean?>(null) }
                var activeRoomId by remember { mutableStateOf<String?>(null) }
                var isAnonymousChat by remember { mutableStateOf(false) }
                var messageText by remember { mutableStateOf("") }
                var isSearching by remember { mutableStateOf(false) }
                var hasRequestedReveal by remember { mutableStateOf(false) }
                var strangerWantsToReveal by remember { mutableStateOf(false) }
                var persistentRooms by remember { mutableStateOf<List<String>>(emptyList()) }
                var inputName by remember { mutableStateOf("") }
                var inputGender by remember { mutableStateOf("Male") }

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
                    }
                }

                // Master Surface that covers the WHOLE screen with DarkBg across state loads
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    when {
                        currentUser == null -> LoginScreen(
                            onLoginClick = { scope.launch { authManager.authenticateWithGoogle() } }
                        )

                        // 1. FIXED: Clean loading structure with matching theme background
                        isProfileComplete == null -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Verifying profile...", color = MaterialTheme.colorScheme.onBackground)
                            }
                        }

                        // 2. FIXED: Removed the padded white Column wrapper entirely!
                        isProfileComplete == false -> {
                            ProfileSetupScreen(
                                inputName = inputName,
                                inputGender = inputGender,
                                onNameChange = { inputName = it },
                                onGenderChange = { inputGender = it },
                                onSaveClick = {
                                    if (inputName.isNotBlank()) {
                                        isProfileComplete = null
                                        profileManager.saveProfile(currentUser.uid, inputName.trim(), inputGender) {
                                            isProfileComplete = true
                                            val manager = ChatManager(appDb.messageDao(), currentUser.uid)
                                            chatManager = manager
                                            manager.listenForPersistentRooms { rooms -> persistentRooms = rooms }
                                        }
                                    }
                                }
                            )
                        }

                        else -> {
                            val manager = chatManager
                            if (manager == null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Loading your chats...")
                                    }
                                }
                            } else if (activeRoomId == null) {
                                InboxScreen(
                                    userId = currentUser.uid,
                                    persistentRooms = persistentRooms,
                                    onAnonymousChatClick = {
                                        isAnonymousChat = true
                                        activeRoomId = "WAITING_FOR_COMMAND"
                                    },
                                    onPersistentRoomClick = { pRoomId ->
                                        manager.listenForMessages(pRoomId)
                                        isAnonymousChat = false
                                        activeRoomId = pRoomId
                                    }
                                )
                            } else {
                                val roomId = activeRoomId!!
                                val messages by manager.getMessagesFlow(roomId).collectAsState(initial = emptyList())

                                ChatScreen(
                                    roomId = roomId,
                                    currentUserId = currentUser.uid,
                                    isAnonymousChat = isAnonymousChat,
                                    isSearching = isSearching,
                                    strangerWantsToReveal = strangerWantsToReveal,
                                    hasRequestedReveal = hasRequestedReveal,
                                    messages = messages,
                                    messageText = messageText,
                                    onMessageChange = { messageText = it },
                                    onBackClick = { activeRoomId = null },
                                    onSendClick = {
                                        val text = messageText.trim()
                                        if (text.isNotBlank()) {
                                            if (isAnonymousChat && text.startsWith("!")) {
                                                when (text.lowercase()) {
                                                    "!start" -> {
                                                        if (roomId == "WAITING_FOR_COMMAND" && !isSearching) {
                                                            isSearching = true
                                                            manager.insertSystemMessage(roomId, "Finding a partner...")
                                                            matchmakingManager.joinQueue(currentUser.uid) { newRoomId ->
                                                                isSearching = false
                                                                manager.insertSystemMessage(newRoomId, "You've connected with a stranger!")
                                                                manager.listenForMessages(newRoomId)
                                                                manager.listenForRevealRequests(newRoomId) { strangerWantsToReveal = true }
                                                                manager.listenForRoomStatus(newRoomId) {
                                                                    manager.insertSystemMessage(newRoomId, "Stranger has disconnected.")
                                                                    activeRoomId = "WAITING_FOR_COMMAND"
                                                                    hasRequestedReveal = false
                                                                    strangerWantsToReveal = false
                                                                }
                                                                activeRoomId = newRoomId
                                                            }
                                                        }
                                                    }
                                                    "!stop" -> {
                                                        if (isSearching) {
                                                            matchmakingManager.leaveQueue(currentUser.uid)
                                                            isSearching = false
                                                            manager.insertSystemMessage(roomId, "Search cancelled.")
                                                        } else if (roomId != "WAITING_FOR_COMMAND") {
                                                            manager.disconnect(roomId)
                                                            activeRoomId = "WAITING_FOR_COMMAND"
                                                            hasRequestedReveal = false
                                                            strangerWantsToReveal = false
                                                        }
                                                    }
                                                    "!reveal" -> {
                                                        if (roomId != "WAITING_FOR_COMMAND") {
                                                            hasRequestedReveal = true
                                                            manager.requestReveal(roomId)
                                                        }
                                                    }
                                                }
                                            } else if (roomId != "WAITING_FOR_COMMAND") {
                                                manager.sendMessage(roomId, text)
                                            }
                                            messageText = ""
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}