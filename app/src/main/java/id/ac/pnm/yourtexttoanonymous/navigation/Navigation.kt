package id.ac.pnm.yourtexttoanonymous.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseUser
import id.ac.pnm.yourtexttoanonymous.data.local.AppDatabase
import id.ac.pnm.yourtexttoanonymous.data.remote.AuthManager
import id.ac.pnm.yourtexttoanonymous.data.remote.ChatManager
import id.ac.pnm.yourtexttoanonymous.data.remote.MatchmakingManager
import id.ac.pnm.yourtexttoanonymous.data.remote.ProfileManager
import id.ac.pnm.yourtexttoanonymous.ui.auth.LoginScreen
import id.ac.pnm.yourtexttoanonymous.ui.profile.ProfileSetupScreen
import id.ac.pnm.yourtexttoanonymous.ui.chat.InboxScreen
import id.ac.pnm.yourtexttoanonymous.ui.chat.ChatScreen
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    startDestination: String,
    currentUser: FirebaseUser?,
    authManager: AuthManager,
    profileManager: ProfileManager,
    matchmakingManager: MatchmakingManager,
    appDb: AppDatabase,
    chatManager: ChatManager?,
    persistentRooms: List<String>,
    onUpdateChatManager: (ChatManager) -> Unit,
    onUpdatePersistentRooms: (List<String>) -> Unit,
    onUpdateProfileComplete: (Boolean?) -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- LOGIN SCREEN ---
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginClick = {
                    scope.launch {
                        authManager.authenticateWithGoogle()
                    }
                }
            )
        }

        // --- PROFILE SETUP SCREEN ---
        composable(Screen.ProfileSetup.route) {
            var inputName by remember { mutableStateOf("") }
            var inputGender by remember { mutableStateOf("Male") }

            ProfileSetupScreen(
                inputName = inputName,
                inputGender = inputGender,
                onNameChange = { inputName = it },
                onGenderChange = { inputGender = it },
                onSaveClick = {
                    if (inputName.isNotBlank() && currentUser != null) {
                        onUpdateProfileComplete(null) // Trigger loading state
                        profileManager.saveProfile(currentUser.uid, inputName.trim(), inputGender) {
                            onUpdateProfileComplete(true)

                            val manager = ChatManager(appDb.messageDao(), currentUser.uid)
                            onUpdateChatManager(manager)
                            manager.listenForPersistentRooms { rooms -> onUpdatePersistentRooms(rooms) }

                            // Pop setup stack out entirely and advance to main inbox view
                            navController.navigate(Screen.Inbox.route) {
                                popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        // --- INBOX SCREEN ---
        composable(Screen.Inbox.route) {
            if (currentUser != null) {
                var userGenderState by remember { mutableStateOf("Male") }

                LaunchedEffect(currentUser.uid) {
                    profileManager.getProfile(currentUser.uid) { profileData ->
                        if (profileData != null) {
                            userGenderState = profileData.gender
                        }
                    }
                }

                InboxScreen(
                    userId = currentUser.uid,
                    userGender = userGenderState,
                    persistentRooms = persistentRooms,
                    chatManager = chatManager, // <-- ADD THIS ONE LINE!
                    onAnonymousChatClick = {
                        navController.navigate(Screen.Chat.createRoute("WAITING_FOR_COMMAND", true))
                    },
                    onPersistentRoomClick = { pRoomId ->
                        chatManager?.listenForMessages(pRoomId)
                        navController.navigate(Screen.Chat.createRoute(pRoomId, false))
                    }
                )
            }
        }

        // --- CHAT SCREEN ---
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("isAnonymous") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val initialRoomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val initialIsAnonymous = backStackEntry.arguments?.getBoolean("isAnonymous") ?: false

            var currentRoomId by remember { mutableStateOf(initialRoomId) }

            // Core Chat States
            var isAnonymousChat by remember { mutableStateOf(initialIsAnonymous) }
            var isSearching by remember { mutableStateOf(false) }
            var hasRequestedReveal by remember { mutableStateOf(false) }
            var strangerWantsToReveal by remember { mutableStateOf(false) }
            var messageText by remember { mutableStateOf("") }

            // Stranger Profile States
            var strangerName by remember { mutableStateOf<String?>(null) }
            var strangerGender by remember { mutableStateOf<String?>(null) }

            val messages by remember(chatManager, currentRoomId) {
                chatManager?.getMessagesFlow(currentRoomId) ?: flowOf(emptyList())
            }.collectAsState(initial = emptyList())

            LaunchedEffect(currentRoomId, isAnonymousChat) {
                // 1. If we are entering a Permanent Chat, fetch the stranger's profile instantly
                if (!isAnonymousChat && currentRoomId != "WAITING_FOR_COMMAND") {
                    chatManager?.getStrangerProfile(currentRoomId) { name, gender ->
                        strangerName = name
                        strangerGender = gender
                    }
                }

                // 2. If we are in an Anonymous Chat, listen for the Reveal Handshake
                if (isAnonymousChat && currentRoomId != "WAITING_FOR_COMMAND") {
                    chatManager?.listenForRevealRequests(
                        roomId = currentRoomId,
                        onStrangerRequested = { strangerWantsToReveal = true },
                        onBothRevealed = {
                            // THE HANDSHAKE! Flip UI instantly
                            isAnonymousChat = false
                            strangerWantsToReveal = false
                            hasRequestedReveal = false

                            chatManager.saveToPersistentRoom(currentRoomId)
                            chatManager.insertSystemMessage(currentRoomId, "Identities revealed! This chat is now saved in your Inbox.")

                            // Fetch their profile data dynamically
                            chatManager.getStrangerProfile(currentRoomId) { name, gender ->
                                strangerName = name
                                strangerGender = gender
                            }
                        }
                    )

                    chatManager?.listenForRoomStatus(currentRoomId) {
                        chatManager.insertSystemMessage(currentRoomId, "Stranger has disconnected.")
                        currentRoomId = "WAITING_FOR_COMMAND"
                        hasRequestedReveal = false
                        strangerWantsToReveal = false
                        strangerName = null // Reset states on disconnect
                        strangerGender = null
                    }
                }
            }

            if (currentUser != null && chatManager != null) {
                ChatScreen(
                    roomId = currentRoomId,
                    currentUserId = currentUser.uid,
                    isAnonymousChat = isAnonymousChat,
                    isSearching = isSearching,
                    strangerWantsToReveal = strangerWantsToReveal,
                    hasRequestedReveal = hasRequestedReveal,
                    strangerName = strangerName,
                    strangerGender = strangerGender,
                    messages = messages,
                    messageText = messageText,
                    onMessageChange = { messageText = it },
                    onBackClick = { navController.popBackStack() },
                    onSendClick = {
                        val text = messageText.trim()
                        if (text.isNotBlank()) {
                            if (isAnonymousChat && text.startsWith("!")) {
                                when (text.lowercase()) {
                                    "!start" -> {
                                        if (currentRoomId == "WAITING_FOR_COMMAND" && !isSearching) {
                                            isSearching = true
                                            chatManager.insertSystemMessage(currentRoomId, "Finding a partner...")
                                            matchmakingManager.joinQueue(currentUser.uid) { newRoomId ->
                                                isSearching = false
                                                chatManager.insertSystemMessage(newRoomId, "You've connected with a stranger!")
                                                chatManager.listenForMessages(newRoomId)
                                                currentRoomId = newRoomId
                                            }
                                        }
                                    }
                                    "!stop" -> {
                                        if (isSearching) {
                                            matchmakingManager.leaveQueue(currentUser.uid)
                                            isSearching = false
                                            chatManager.insertSystemMessage(currentRoomId, "Search cancelled.")
                                        } else if (currentRoomId != "WAITING_FOR_COMMAND") {
                                            chatManager.disconnect(currentRoomId)
                                            currentRoomId = "WAITING_FOR_COMMAND"
                                            hasRequestedReveal = false
                                            strangerWantsToReveal = false
                                            strangerName = null
                                            strangerGender = null
                                        }
                                    }
                                    "!reveal" -> {
                                        if (currentRoomId != "WAITING_FOR_COMMAND") {
                                            hasRequestedReveal = true
                                            chatManager.requestReveal(currentRoomId)
                                        }
                                    }
                                }
                            } else if (currentRoomId != "WAITING_FOR_COMMAND") {
                                chatManager.sendMessage(currentRoomId, text)
                            }
                            messageText = ""
                        }
                    }
                )
            }
        }
    }
}