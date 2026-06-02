package id.ac.pnm.yourtexttoanonymous

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.ac.pnm.yourtexttoanonymous.ui.theme.YourTextToAnonymousTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val authManager by lazy { AuthManager(this) }
    private val matchmakingManager = MatchmakingManager()
    private val profileManager = ProfileManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appDb = AppDatabase.getDatabase(this)

        setContent {
            YourTextToAnonymousTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    val scope = rememberCoroutineScope()
                    val currentUser = authManager.getCurrentUser()
                    
                    var chatManager by remember { mutableStateOf<ChatManager?>(null) }
                    var isProfileComplete by remember { mutableStateOf<Boolean?>(null) } // null = checking, false = needs setup, true = done
                    
                    var activeRoomId by remember { mutableStateOf<String?>(null) }
                    var isAnonymousChat by remember { mutableStateOf(false) } 
                    var messageText by remember { mutableStateOf("") }
                    
                    var hasRequestedReveal by remember { mutableStateOf(false) }
                    var strangerWantsToReveal by remember { mutableStateOf(false) }
                    var persistentRooms by remember { mutableStateOf<List<String>>(emptyList()) }

                    // Profile Registration Form State
                    var inputName by remember { mutableStateOf("") }
                    var inputGender by remember { mutableStateOf("Male") }

                    // 1. Check Profile Status on Login
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

                    if (currentUser == null) {
                        // STATE: NOT LOGGED IN
                        Text(text = "Status: Not Authenticated")
                        Button(onClick = { scope.launch { authManager.authenticateWithGoogle() } }) {
                            Text("Trigger Google Login")
                        }
                    } else if (isProfileComplete == null) {
                        // STATE: LOADING PROFILE STATUS
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Text("Verifying profile...")
                    } else if (isProfileComplete == false) {
                        // STATE: NEW USER (NEEDS REGISTRATION)
                        Text("Complete Your Profile", style = MaterialTheme.typography.headlineSmall)
                        Text("Set up your identity before meeting strangers.", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 24.dp))
                        
                        TextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Display Name / Username") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            singleLine = true
                        )
                        
                        Text("Gender:")
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                            RadioButton(selected = inputGender == "Male", onClick = { inputGender = "Male" })
                            Text("Male")
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = inputGender == "Female", onClick = { inputGender = "Female" })
                            Text("Female")
                        }
                        
                        Button(
                            onClick = {
                                if (inputName.isNotBlank()) {
                                    isProfileComplete = null // Trigger loading spinner
                                    profileManager.saveProfile(currentUser.uid, inputName.trim(), inputGender) {
                                        // Once saved, initialize chat and move to Inbox
                                        isProfileComplete = true
                                        val manager = ChatManager(appDb.messageDao(), currentUser.uid)
                                        chatManager = manager
                                        manager.listenForPersistentRooms { rooms -> persistentRooms = rooms }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Profile & Continue")
                        }

                    } else {
                        // STATE: FULLY LOGGED IN & REGISTERED (INBOX / CHAT)
                        val manager = chatManager
                        if (manager == null) {
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                            Text("Loading your chats...")
                            return@Column
                        }

                        if (activeRoomId == null) {
                            // --- INBOX SCREEN ---
                            Text(text = "Logged in as: ${currentUser.uid}", style = MaterialTheme.typography.bodySmall)
                            
                            Text("Your Inbox", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable {
                                    isAnonymousChat = true
                                    activeRoomId = "WAITING_FOR_COMMAND" 
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text(text = "🕵️ Anonymous Roomchat", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                            }

                            if (persistentRooms.isEmpty()) {
                                Text("No permanent chats yet. Reveal your identity with someone to add them here!", color = MaterialTheme.colorScheme.secondary)
                            } else {
                                LazyColumn {
                                    items(persistentRooms) { pRoomId ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                                manager.listenForMessages(pRoomId)
                                                isAnonymousChat = false
                                                activeRoomId = pRoomId
                                            }
                                        ) {
                                            Text(text = "Chat: $pRoomId", modifier = Modifier.padding(16.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            // --- ACTIVE CHAT SCREEN ---
                            val roomId = activeRoomId!!
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isAnonymousChat) {
                                    val statusText = if (activeRoomId == "WAITING_FOR_COMMAND") "Anonymous Roomchat" else "Chatting (Type !stop to leave)"
                                    Text(statusText, style = MaterialTheme.typography.titleMedium)
                                    Button(onClick = { activeRoomId = null }) { Text("Back") }
                                } else {
                                    Text("Permanent Chat", style = MaterialTheme.typography.titleMedium)
                                    Button(onClick = { activeRoomId = null }) { Text("Back") }
                                }
                            }

                            if (isAnonymousChat && strangerWantsToReveal && !hasRequestedReveal) {
                                Text("Stranger wants to reveal! Type !reveal to accept.", color = MaterialTheme.colorScheme.primary)
                            }

                            val messages by manager.getMessagesFlow(roomId).collectAsState(initial = emptyList())

                            LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                                items(messages) { msg ->
                                    val alignment = if (msg.senderId == currentUser.uid) "You" else if (isAnonymousChat) "Stranger" else "Friend"
                                    Text("$alignment: ${msg.text}")
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                TextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val text = messageText.trim()
                                        if (text.isNotBlank()) {
                                            if (isAnonymousChat && text.startsWith("!")) {
                                                when (text.lowercase()) {
                                                    "!start" -> {
                                                        if (roomId == "WAITING_FOR_COMMAND") {
                                                            matchmakingManager.joinQueue(currentUser.uid) { newRoomId ->
                                                                manager.listenForMessages(newRoomId)
                                                                manager.listenForRevealRequests(newRoomId) { strangerWantsToReveal = true }
                                                                manager.listenForRoomStatus(newRoomId) {
                                                                    activeRoomId = "WAITING_FOR_COMMAND"
                                                                    hasRequestedReveal = false
                                                                    strangerWantsToReveal = false
                                                                }
                                                                activeRoomId = newRoomId
                                                            }
                                                        }
                                                    }
                                                    "!stop" -> {
                                                        if (roomId != "WAITING_FOR_COMMAND") {
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
                                    },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("Send")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
