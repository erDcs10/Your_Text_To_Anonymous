package id.ac.pnm.yourtexttoanonymous.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.pnm.yourtexttoanonymous.data.remote.ChatManager
import id.ac.pnm.yourtexttoanonymous.ui.theme.*

@Composable
fun InboxScreen(
    userId: String,
    userGender: String,
    persistentRooms: List<String>,
    chatManager: ChatManager?,
    onAnonymousChatClick: () -> Unit,
    onPersistentRoomClick: (String) -> Unit
) {
    val roomProfiles = remember { mutableStateMapOf<String, Pair<String, String>>() }

    LaunchedEffect(persistentRooms, chatManager) {
        persistentRooms.forEach { roomId ->
            if (!roomProfiles.containsKey(roomId)) {
                chatManager?.getStrangerProfile(roomId) { name, gender ->
                    if (name != null && gender != null) {
                        roomProfiles[roomId] = Pair(name, gender)
                    }
                }
            }
        }
    }

    // --- DYNAMIC CARD COLOR ---
    // We check the logged-in user's gender to theme their Anonymous Card
    val isUserFemale = userGender.equals("Female", ignoreCase = true)
    val cardThemeColor = if (isUserFemale) Pink80 else GoogleBlue

    Scaffold(
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            Text(
                text = "Inbox",
                color = WhiteBg,
                fontFamily = LuxoraGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier.padding(top = 32.dp, bottom = 24.dp)
            )

            // --- THE ANONYMOUS CHAT CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .clickable { onAnonymousChatClick() },
                // Apply the dynamic color to the card background
                colors = CardDefaults.cardColors(containerColor = cardThemeColor.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🕵️",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Find a Stranger",
                            color = cardThemeColor, // Apply the dynamic color to the title
                            fontFamily = LuxoraGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Start an anonymous chat",
                            color = WhiteBg.copy(alpha = 0.7f),
                            fontFamily = LuxoraGroteskFamily,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = "Your Connections",
                color = WhiteBg.copy(alpha = 0.5f),
                fontFamily = LuxoraGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // --- SAVED ROOMS LIST ---
            if (persistentRooms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No revealed connections yet.\nReveal your identity to save chats here!",
                        color = WhiteBg.copy(alpha = 0.3f),
                        fontFamily = LuxoraGroteskFamily,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(persistentRooms) { roomId ->
                        val profile = roomProfiles[roomId]
                        val displayName = profile?.first ?: "Loading..."
                        val displayGender = profile?.second ?: "..."

                        InboxItem(
                            name = displayName,
                            gender = displayGender,
                            onClick = { onPersistentRoomClick(roomId) }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun InboxItem(
    name: String,
    gender: String,
    onClick: () -> Unit
) {
    val isFemale = gender.equals("Female", ignoreCase = true)
    val avatarBg = if (isFemale) Pink80 else GoogleBlue
    val subtitleColor = if (isFemale) Pink80.copy(alpha = 0.8f) else GoogleBlue.copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WhiteBg.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = WhiteBg,
                fontFamily = LuxoraGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = name,
                color = WhiteBg,
                fontFamily = LuxoraGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Revealed • $gender",
                color = subtitleColor,
                fontFamily = LuxoraGroteskFamily,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}