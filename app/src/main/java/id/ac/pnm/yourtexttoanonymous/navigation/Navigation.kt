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
import com.google.firebase.auth.FirebaseAuth

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

    var firebaseUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var currentUserGender by remember { mutableStateOf("Male") }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { authState ->
            firebaseUser = authState.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(firebaseUser) {
        val user = firebaseUser
        if (user != null) {
            profileManager.getProfile(user.uid) { profileData ->
                if (profileData != null) {

                    currentUserGender = profileData.gender ?: "Male"

                    val manager = ChatManager(appDb.messageDao(), user.uid)
                    onUpdateChatManager(manager)
                    manager.listenForPersistentRooms { rooms -> onUpdatePersistentRooms(rooms) }

                    navController.navigate(Screen.Inbox.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.ProfileSetup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
        } else {
            currentUserGender = "Male"

            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (firebaseUser == null) Screen.Login.route else Screen.Inbox.route
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
                    val user = firebaseUser
                    if (inputName.isNotBlank() && user != null) {
                        onUpdateProfileComplete(null) // Trigger loading spinner

                        profileManager.saveProfile(user.uid, inputName.trim(), inputGender) {
                            onUpdateProfileComplete(true)

                            val manager = ChatManager(appDb.messageDao(), user.uid)
                            onUpdateChatManager(manager)
                            manager.listenForPersistentRooms { rooms -> onUpdatePersistentRooms(rooms) }

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
            val user = firebaseUser // Use the reactive state here!
            if (user != null) {
                InboxScreen(
                    userId = user.uid,
                    userGender = currentUserGender, // Pass the global state directly!
                    persistentRooms = persistentRooms,
                    chatManager = chatManager,
                    onAnonymousChatClick = {
                        navController.navigate(Screen.Chat.createRoute("WAITING_FOR_COMMAND", true))
                    },
                    onPersistentRoomClick = { pRoomId ->
                        chatManager?.listenForMessages(pRoomId)
                        navController.navigate(Screen.Chat.createRoute(pRoomId, false))
                    },
                    onDeleteRoomClick = { roomIdToTrash ->
                        chatManager?.requestDeleteRoom(roomIdToTrash)
                    },
                    onLogoutClick = {
                        authManager.requestLogout(user.uid) {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
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
                if (!isAnonymousChat && currentRoomId != "WAITING_FOR_COMMAND") {
                    chatManager?.getStrangerProfile(currentRoomId) { name, gender ->
                        strangerName = name
                        strangerGender = gender
                    }
                }

                if (isAnonymousChat && currentRoomId != "WAITING_FOR_COMMAND") {
                    chatManager?.listenForRevealRequests(
                        roomId = currentRoomId,
                        onStrangerRequested = { strangerWantsToReveal = true },
                        onBothRevealed = {
                            isAnonymousChat = false
                            strangerWantsToReveal = false
                            hasRequestedReveal = false

                            chatManager.saveToPersistentRoom(currentRoomId)
                            chatManager.insertSystemMessage(currentRoomId, "Identities revealed! This chat is now saved in your Inbox.")

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
                        strangerName = null
                        strangerGender = null
                    }
                }
            }

            val user = firebaseUser
            if (user != null && chatManager != null) {
                ChatScreen(
                    roomId = currentRoomId,
                    currentUserId = user.uid,
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
                                            matchmakingManager.joinQueue(user.uid) { newRoomId ->
                                                isSearching = false
                                                chatManager.insertSystemMessage(newRoomId, "You've connected with a stranger!")
                                                chatManager.listenForMessages(newRoomId)
                                                currentRoomId = newRoomId
                                            }
                                        }
                                    }
                                    "!stop" -> {
                                        if (isSearching) {
                                            matchmakingManager.leaveQueue(user.uid)
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