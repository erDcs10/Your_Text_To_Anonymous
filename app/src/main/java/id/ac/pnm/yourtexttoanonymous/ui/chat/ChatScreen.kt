package id.ac.pnm.yourtexttoanonymous.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.ac.pnm.yourtexttoanonymous.data.local.entity.MessageEntity


@Composable
fun ChatScreen(
    roomId: String,
    currentUserId: String,
    isAnonymousChat: Boolean,
    isSearching: Boolean,
    strangerWantsToReveal: Boolean,
    hasRequestedReveal: Boolean,
    messages: List<MessageEntity>,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusText = when {
                !isAnonymousChat -> "Permanent Chat"
                roomId == "WAITING_FOR_COMMAND" && isSearching -> "Searching for partner..."
                roomId == "WAITING_FOR_COMMAND" -> "Anonymous Roomchat"
                else -> "Chatting (Type !stop to leave)"
            }
            Text(statusText, style = MaterialTheme.typography.titleMedium)
            Button(onClick = onBackClick) { Text("Back") }
        }

        if (isAnonymousChat && strangerWantsToReveal && !hasRequestedReveal) {
            Text(
                "Stranger wants to reveal! Type !reveal to accept.",
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            items(messages) { msg ->
                val alignment = when (msg.senderId) {
                    currentUserId -> "You"
                    "SYSTEM" -> "System"
                    else -> if (isAnonymousChat) "Stranger" else "Friend"
                }
                val textColor = if (msg.senderId == "SYSTEM")
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface

                Text("$alignment: ${msg.text}", color = textColor)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = messageText,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onSendClick,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Send")
            }
        }
    }
}