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

    private var messagesListener: ChildEventListener? = null
    private var roomStatusListener: com.google.firebase.database.ValueEventListener? = null

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

        messagesListener = object : ChildEventListener {
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
        }
        messagesRef.addChildEventListener(messagesListener!!)
    }

    fun listenForRoomStatus(roomId: String, onRoomEnded: () -> Unit) {
        val statusRef = db.child("rooms").child(roomId).child("status")
        
        roomStatusListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.getValue(String::class.java) == "ended") {
                    cleanup(roomId)
                    onRoomEnded()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        statusRef.addValueEventListener(roomStatusListener!!)
    }

    fun disconnect(roomId: String) {
        db.child("rooms").child(roomId).child("status").setValue("ended")
        cleanup(roomId)
    }

    private fun cleanup(roomId: String) {
        messagesListener?.let { db.child("rooms").child(roomId).child("messages").removeEventListener(it) }
        roomStatusListener?.let { db.child("rooms").child(roomId).child("status").removeEventListener(it) }
        
        db.child("users").child(currentUserId).child("activeRoom").removeValue()
    }

    fun getMessagesFlow(roomId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForRoom(roomId)
    }
}
