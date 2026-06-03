package id.ac.pnm.yourtexttoanonymous

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val roomId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val isSeen: Boolean = false
)
