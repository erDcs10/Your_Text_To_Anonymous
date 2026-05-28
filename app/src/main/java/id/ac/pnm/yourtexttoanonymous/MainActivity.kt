package id.ac.pnm.yourtexttoanonymous

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    private lateinit var chatManager: ChatManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appDb = AppDatabase.getDatabase(this)

        setContent {
            YourTextToAnonymousTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 48.dp, start = 16.dp, end = 16.dp)
                ) {
                    val scope = rememberCoroutineScope()
                    val currentUser = authManager.getCurrentUser()
                    var activeRoomId by remember { mutableStateOf<String?>(null) }
                    var messageText by remember { mutableStateOf("") }
                    var hasRequestedReveal by remember { mutableStateOf(false) }
                    var strangerWantsToReveal by remember { mutableStateOf(false) }

                    if (currentUser == null) {
                        Text(text = "Status: Not Authenticated")
                        Button(onClick = {
                            scope.launch {
                                authManager.authenticateWithGoogle()
                            }
                        }) {
                            Text("Trigger Google Login")
                        }
                    } else {
                        if (activeRoomId == null) {
                            Text(text = "UID: ${currentUser.uid}")
                            Button(
                                onClick = {
                                    matchmakingManager.joinQueue(currentUser.uid) { roomId ->
                                        chatManager = ChatManager(appDb.messageDao(), currentUser.uid)
                                        chatManager.listenForMessages(roomId)
                                        chatManager.listenForRevealRequests(roomId) {
                                            // TODO handle reveal request
                                        }
                                        chatManager.listenForRoomStatus(roomId) {
                                            activeRoomId = null
                                        }
                                        activeRoomId = roomId
                                    }
                                },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Find Anonymous Match")
                            }
                        } else {
                            val roomId = activeRoomId!!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                if (strangerWantsToReveal && !hasRequestedReveal) {
                                    Button(onClick = {
                                        hasRequestedReveal = true
                                        chatManager.requestReveal(roomId)
                                    }) { Text("Accept Reveal!") }
                                } else if (!hasRequestedReveal) {
                                    Button(onClick = {
                                        hasRequestedReveal = true
                                        chatManager.requestReveal(roomId)
                                    }) { Text("!reveal") }
                                } else {
                                    Text("Reveal Requested...", color = MaterialTheme.colorScheme.primary)
                                }

                                Button(
                                    onClick = {
                                        chatManager.disconnect(roomId)
                                        activeRoomId = null
                                        hasRequestedReveal = false
                                        strangerWantsToReveal = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Disconnect")
                                }
                            }
                            val messages by chatManager.getMessagesFlow(roomId).collectAsState(initial = emptyList())

                            LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                                items(messages) { msg ->
                                    val alignment = if (msg.senderId == currentUser.uid) "You" else "Stranger"
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
                                            chatManager.sendMessage(roomId, messageText)
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
