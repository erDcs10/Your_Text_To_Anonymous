package id.ac.pnm.yourtexttoanonymous.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InboxScreen(
    userId: String,
    persistentRooms: List<String>,
    onAnonymousChatClick: () -> Unit,
    onPersistentRoomClick: (String) -> Unit
) {
    Text(text = "Logged in as: $userId", style = MaterialTheme.typography.bodySmall)
    Text("Your Inbox", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable { onAnonymousChatClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            text = "🕵️ Anonymous Roomchat",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }

    if (persistentRooms.isEmpty()) {
        Text(
            "No permanent chats yet. Reveal your identity with someone to add them here!",
            color = MaterialTheme.colorScheme.secondary
        )
    } else {
        LazyColumn {
            items(persistentRooms) { pRoomId ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onPersistentRoomClick(pRoomId) }
                ) {
                    Text(text = "Chat: $pRoomId", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}