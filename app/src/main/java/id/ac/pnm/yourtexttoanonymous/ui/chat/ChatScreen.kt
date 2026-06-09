package id.ac.pnm.yourtexttoanonymous.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.pnm.yourtexttoanonymous.data.local.entity.MessageEntity
import id.ac.pnm.yourtexttoanonymous.ui.theme.*

@Composable
fun ChatScreen(
    roomId: String,
    currentUserId: String,
    isAnonymousChat: Boolean,
    isSearching: Boolean,
    strangerWantsToReveal: Boolean,
    hasRequestedReveal: Boolean,
    strangerName: String?,
    strangerGender: String?,
    messages: List<MessageEntity>,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = DarkBg,
        topBar = {
            ChatHeader(
                roomId = roomId,
                isAnonymousChat = isAnonymousChat,
                isSearching = isSearching,
                strangerName = strangerName,
                strangerGender = strangerGender,
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Column {
                if (isAnonymousChat && strangerWantsToReveal && !hasRequestedReveal) {
                    RevealBanner()
                }
                ChatInputBar(
                    messageText = messageText,
                    onMessageChange = onMessageChange,
                    onSendClick = onSendClick
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(
                    message = msg,
                    currentUserId = currentUserId
                )
            }
        }
    }
}

@Composable
fun ChatHeader(
    roomId: String,
    isAnonymousChat: Boolean,
    isSearching: Boolean,
    strangerName: String?,
    strangerGender: String?,
    onBackClick: () -> Unit
) {
    // Dynamically choose the title
    val title = when {
        !isAnonymousChat && strangerName != null -> strangerName
        !isAnonymousChat -> "Room $roomId"
        roomId == "WAITING_FOR_COMMAND" -> "Anonymous Chat"
        else -> "Stranger"
    }

    // Dynamically choose the subtitle
    val subtitle = when {
        !isAnonymousChat && strangerGender != null -> "Revealed • $strangerGender"
        !isAnonymousChat -> "Revealed Connection"
        roomId == "WAITING_FOR_COMMAND" && isSearching -> "Searching for partner..."
        roomId == "WAITING_FOR_COMMAND" -> "Type !start to find someone"
        else -> "Type !stop to disconnect"
    }

    // Color the Avatar based on gender
    val avatarBg = when {
        isSearching -> GoogleBlue.copy(alpha = 0.2f)
        !isAnonymousChat && strangerGender.equals("Female", ignoreCase = true) -> Pink80
        !isAnonymousChat && strangerGender.equals("Male", ignoreCase = true) -> GoogleBlue
        else -> WhiteBg.copy(alpha = 0.1f)
    }

    // Color the Subtitle text based on gender
    val subtitleColor = when {
        isSearching -> GoogleBlue
        !isAnonymousChat && strangerGender.equals("Female", ignoreCase = true) -> Pink80
        !isAnonymousChat && strangerGender.equals("Male", ignoreCase = true) -> GoogleBlue
        else -> WhiteBg.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBg.copy(alpha = 0.95f))
            .padding(top = 48.dp, bottom = 16.dp, start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = WhiteBg
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isAnonymousChat) "🕵️" else title.take(1).uppercase(),
                color = if (isAnonymousChat) Color.Unspecified else WhiteBg,
                fontFamily = LuxoraGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                color = WhiteBg,
                fontFamily = LuxoraGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = subtitle,
                color = subtitleColor,
                fontFamily = LuxoraGroteskFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    currentUserId: String
) {
    val isMe = message.senderId == currentUserId
    val isSystem = message.senderId == "SYSTEM"

    if (isSystem) {
        // System Message Centered
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.text,
                color = WhiteBg.copy(alpha = 0.4f),
                fontFamily = LuxoraGroteskFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(WhiteBg.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    } else {
        // User/Stranger Messages
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isMe) 20.dp else 4.dp, // Sharp corner points to sender
                            bottomEnd = if (isMe) 4.dp else 20.dp
                        )
                    )
                    .background(if (isMe) GoogleBlue else WhiteBg.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    color = WhiteBg,
                    fontFamily = LuxoraGroteskFamily,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.Bottom
    ) {
        // Custom Pill-shaped text field
        BasicTextField(
            value = messageText,
            onValueChange = onMessageChange,
            textStyle = TextStyle(
                color = WhiteBg,
                fontFamily = LuxoraGroteskFamily,
                fontSize = 16.sp
            ),
            cursorBrush = SolidColor(GoogleBlue),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(WhiteBg.copy(alpha = 0.05f))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            decorationBox = { innerTextField ->
                if (messageText.isEmpty()) {
                    Text(
                        text = "Type a message...",
                        color = WhiteBg.copy(alpha = 0.3f),
                        fontFamily = LuxoraGroteskFamily
                    )
                }
                innerTextField()
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Sleek Send Button
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (messageText.isNotBlank()) GoogleBlue else WhiteBg.copy(alpha = 0.1f))
                .clickable(enabled = messageText.isNotBlank()) { onSendClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (messageText.isNotBlank()) WhiteBg else WhiteBg.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = 2.dp) // Optical alignment for send icon
            )
        }
    }
}

@Composable
fun RevealBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Pink80.copy(alpha = 0.15f))
            .border(1.dp, Pink80.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Stranger wants to reveal!",
                color = Pink80,
                fontFamily = LuxoraGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Type !reveal to accept and move to Inbox",
                color = WhiteBg.copy(alpha = 0.7f),
                fontFamily = LuxoraGroteskFamily,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}