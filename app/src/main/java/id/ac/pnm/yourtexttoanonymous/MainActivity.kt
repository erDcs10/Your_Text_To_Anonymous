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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.ac.pnm.yourtexttoanonymous.ui.theme.YourTextToAnonymousTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val authManager by lazy { AuthManager(this) }
    private val matchmakingManager = MatchmakingManager()
    // private var chatManager: ChatManager? = null

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
                    var activeRoomId by remember { mutableStateOf<String?>(null) }
                    var isAnonymousChat by remember { mutableStateOf(false) } 
                    var messageText by remember { mutableStateOf("") }
                    
                    var hasRequestedReveal by remember { mutableStateOf(false) }
                    var strangerWantsToReveal by remember { mutableStateOf(false) }
                    var persistentRooms by remember { mutableStateOf<List<String>>(emptyList()) }

                    LaunchedEffect(currentUser) {
                        if (currentUser != null) {
                            val manager = ChatManager(appDb.messageDao(), currentUser.uid)
                            chatManager = manager
                            manager.listenForPersistentRooms { rooms ->
                                persistentRooms = rooms
                            }
                        }
                    }

                    if (currentUser == null) {
                        Text(text = "Status: Not Authenticated")
                        Button(onClick = { scope.launch { authManager.authenticateWithGoogle() } }) {
                            Text("Trigger Google Login")
                        }
                    } else {
                        val manager = chatManager
                        if (manager == null) {
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                            Text("Loading your chats...")
                            return@Column
                        }

                        if (activeRoomId == null) {
                            Text(text = "Logged in as: ${currentUser.uid}", style = MaterialTheme.typography.bodySmall)
                            
                            Button(
                                onClick = {
                                    matchmakingManager.joinQueue(currentUser.uid) { roomId ->
                                        manager.listenForMessages(roomId)
                                        manager.listenForRevealRequests(roomId) {
                                            strangerWantsToReveal = true
                                        }
                                        manager.listenForRoomStatus(roomId) {
                                            activeRoomId = null
                                            hasRequestedReveal = false
                                            strangerWantsToReveal = false
                                        }
                                        isAnonymousChat = true
                                        activeRoomId = roomId
                                    }
                                },
                                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp).fillMaxWidth()
                            ) {
                                Text("Find Anonymous Match")
                            }

                            Text("Your Inbox (Revealed Chats)", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                            val roomId = activeRoomId!!
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                if (isAnonymousChat) {
                                    if (strangerWantsToReveal && !hasRequestedReveal) {
                                        Button(onClick = {
                                            hasRequestedReveal = true
                                            manager.requestReveal(roomId)
                                        }) { Text("Accept Reveal!") }
                                    } else if (!hasRequestedReveal) {
                                        Button(onClick = {
                                            hasRequestedReveal = true
                                            manager.requestReveal(roomId)
                                        }) { Text("!reveal") }
                                    } else {
                                        Text("Reveal Requested...", color = MaterialTheme.colorScheme.primary)
                                    }

                                    Button(
                                        onClick = {
                                            manager.disconnect(roomId)
                                            activeRoomId = null
                                            hasRequestedReveal = false
                                            strangerWantsToReveal = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Disconnect")
                                    }
                                } else {
                                    Text("Permanent Chat", style = MaterialTheme.typography.titleMedium)
                                    Button(onClick = { activeRoomId = null }) {
                                        Text("Back to Inbox")
                                    }
                                }
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
                                        if (messageText.isNotBlank()) {
                                            manager.sendMessage(roomId, messageText)
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
