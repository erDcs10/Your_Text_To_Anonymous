package id.ac.pnm.yourtexttoanonymous.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.pnm.yourtexttoanonymous.R
import id.ac.pnm.yourtexttoanonymous.data.remote.ChatManager
import id.ac.pnm.yourtexttoanonymous.ui.theme.*

@Composable
fun InboxScreen(
    userId: String,
    userGender: String,
    persistentRooms: List<String>,
    chatManager: ChatManager?,
    onAnonymousChatClick: () -> Unit,
    onPersistentRoomClick: (String) -> Unit,
    onDeleteRoomClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    val roomProfiles = remember { mutableStateMapOf<String, Pair<String, String>>() }

    // --- NEW: State to hold the latest message for each room ---
    val roomLatestMessages = remember { mutableStateMapOf<String, String>() }

    var showDropdownMenu by remember { mutableStateOf(false) }
    var roomToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(persistentRooms, chatManager) {
        persistentRooms.forEach { roomId ->
            // Fetch Profile
            if (!roomProfiles.containsKey(roomId)) {
                chatManager?.getStrangerProfile(roomId) { name, gender ->
                    if (name != null && gender != null) {
                        roomProfiles[roomId] = Pair(name, gender)
                    }
                }
            }
            // --- NEW: Listen for the latest message in real-time ---
            chatManager?.listenForLatestMessage(roomId) { latestMsg ->
                if (latestMsg != null) {
                    roomLatestMessages[roomId] = latestMsg
                }
            }
        }
    }

    val isUserFemale = userGender.equals("Female", ignoreCase = true)
    val cardThemeColor = if (isUserFemale) Pink80 else GoogleBlue

    if (roomToDelete != null) {
        val profile = roomProfiles[roomToDelete]
        val name = profile?.first ?: "this connection"

        AlertDialog(
            onDismissRequest = { roomToDelete = null },
            containerColor = DarkBg,
            title = { Text("Delete Chat?", color = WhiteBg, fontFamily = LuxoraGroteskFamily) },
            text = { Text("Are you sure you want to permanently delete your chat with $name?", color = WhiteBg.copy(alpha = 0.7f), fontFamily = LuxoraGroteskFamily) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteRoomClick(roomToDelete!!)
                    roomToDelete = null
                }) {
                    Text("Delete", color = Pink80, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { roomToDelete = null }) {
                    Text("Cancel", color = GoogleBlue)
                }
            }
        )
    }

    Scaffold(
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ytta_text_simplified),
                    contentDescription = "YTTA Text Icon",
                    modifier = Modifier.height(32.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart
                )

                Box {
                    IconButton(onClick = { showDropdownMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = WhiteBg)
                    }
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false },
                        modifier = Modifier.background(WhiteBg.copy(alpha = 0.1f))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Logout", color = Pink80, fontFamily = LuxoraGroteskFamily) },
                            onClick = {
                                showDropdownMenu = false
                                onLogoutClick()
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .clickable { onAnonymousChatClick() },
                colors = CardDefaults.cardColors(containerColor = cardThemeColor.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_searching),
                        contentDescription = "Stranger Icon",
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Find a Stranger",
                            color = cardThemeColor,
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

            if (persistentRooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No revealed connections yet.\nReveal your identity to save chats here!",
                        color = WhiteBg.copy(alpha = 0.3f),
                        fontFamily = LuxoraGroteskFamily,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(persistentRooms) { roomId ->
                        val profile = roomProfiles[roomId]
                        val displayName = profile?.first ?: "Loading..."
                        val displayGender = profile?.second ?: "..."

                        // --- NEW: Grab the latest message or show a default ---
                        val displayMessage = roomLatestMessages[roomId] ?: "No messages yet"

                        InboxItem(
                            name = displayName,
                            gender = displayGender,
                            latestMessage = displayMessage, // PASS IT TO THE UI
                            onClick = { onPersistentRoomClick(roomId) },
                            onLongClick = { roomToDelete = roomId }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InboxItem(
    name: String,
    gender: String,
    latestMessage: String, // --- NEW PARAMETER ---
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // We still need gender to color the avatar properly
    val isFemale = gender.equals("Female", ignoreCase = true)
    val avatarBg = if (isFemale) Pink80 else GoogleBlue
    val subtitleColor = WhiteBg.copy(alpha = 0.7f) // Subdued color for messages

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WhiteBg.copy(alpha = 0.05f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(text = name.take(1).uppercase(), color = WhiteBg, fontFamily = LuxoraGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) { // Add weight to stop text from pushing out of bounds
            Text(text = name, color = WhiteBg, fontFamily = LuxoraGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // --- NEW: LATEST MESSAGE UI ---
            Text(
                text = latestMessage,
                color = subtitleColor,
                fontFamily = LuxoraGroteskFamily,
                fontSize = 13.sp,
                maxLines = 1, // Restrict to one line
                overflow = TextOverflow.Ellipsis, // Add "..." if too long
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}