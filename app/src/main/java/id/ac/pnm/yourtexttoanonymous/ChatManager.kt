package id.ac.pnm.yourtexttoanonymous

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatManager(
    private val messageDao: MessageDao,
    private val currentUserId: String
) {
    private val db = FirebaseDatabase.getInstance().reference

    fun sendMessage(roomId: String, text: String) {
        val messageId = UUID.randomUUID().toString()
        val messageRef = db.child("rooms").child(roomId).child("messages").child(messageId)

        val messageData = mapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "text" to text,
            "timestamp" to ServerValue.TIMESTAMP,
            "isSeen" to false
        )

        messageRef.setValue(messageData)
            .addOnSuccessListener { Log.d("ChatManager", "Message sent") }
            .addOnFailureListener { Log.e("ChatManager", "Failed", it) }
    }

    fun listenForMessages(roomId: String) {
        val messagesRef = db.child("rooms").child(roomId).child("messages")

        messagesRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val msgMap = snapshot.value as? Map<*, *> ?: return
                
                val messageId = msgMap["messageId"] as? String ?: return
                val senderId = msgMap["senderId"] as? String ?: return
                val text = msgMap["text"] as? String ?: return
                val timestamp = msgMap["timestamp"] as? Long ?: System.currentTimeMillis()
                val isSeen = msgMap["isSeen"] as? Boolean ?: false

                val entity = MessageEntity(
                    messageId = messageId,
                    roomId = roomId,
                    senderId = senderId,
                    text = text,
                    timestamp = timestamp,
                    isSeen = isSeen
                )

                CoroutineScope(Dispatchers.IO).launch {
                    messageDao.insertMessage(entity)
                    
                    if (senderId != currentUserId && !isSeen) {
                        snapshot.ref.child("isSeen").setValue(true)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun getMessagesFlow(roomId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForRoom(roomId)
    }
}
