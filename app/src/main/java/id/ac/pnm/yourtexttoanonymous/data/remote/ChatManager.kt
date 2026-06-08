package id.ac.pnm.yourtexttoanonymous.data.remote

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import id.ac.pnm.yourtexttoanonymous.data.local.dao.MessageDao
import id.ac.pnm.yourtexttoanonymous.data.local.entity.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.collections.get

class ChatManager(
    private val messageDao: MessageDao,
    private val currentUserId: String
) {
    private val db = FirebaseDatabase.getInstance().reference

    private var messagesListener: ChildEventListener? = null
    private var roomStatusListener: ValueEventListener? = null

    fun sendMessage(roomId: String, text: String, isAnonymousChat: Boolean = false) {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val msgMap = mapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "text" to text,
            "timestamp" to timestamp,
            "isSeen" to false
        )

        db.child("rooms").child(roomId).child("messages").child(messageId).setValue(msgMap)

        if (!isAnonymousChat) {
            val notifReq = mapOf(
                "roomId" to roomId,
                "senderId" to currentUserId,
                "text" to text
            )
            db.child("notificationRequests").push().setValue(notifReq)
        }
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
        
        roomStatusListener = object : ValueEventListener {
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

    fun requestReveal(roomId: String) {
        db.child("rooms").child(roomId).child("revealRequests").child(currentUserId).setValue(true)
    }

    fun listenForRevealRequests(roomId: String, onStrangerRequested: () -> Unit) {
        val revealRef = db.child("rooms").child(roomId).child("revealRequests")
        
        revealRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val strangerRequested = snapshot.children.any { child ->
                    child.key != currentUserId && (child.value as? Boolean) == true
                }
                
                if (strangerRequested) {
                    onStrangerRequested()
                }
            }
            
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenForPersistentRooms(onRoomsUpdated: (List<String>) -> Unit) {
        db.child("users").child(currentUserId).child("persistentRooms")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val rooms = snapshot.children.mapNotNull { it.key }
                    onRoomsUpdated(rooms)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun insertSystemMessage(roomId: String, text: String) {
        val entity = MessageEntity(
            messageId = UUID.randomUUID().toString(),
            roomId = roomId,
            senderId = "SYSTEM",
            text = text,
            timestamp = System.currentTimeMillis(),
            isSeen = true
        )
        CoroutineScope(Dispatchers.IO).launch {
            messageDao.insertMessage(entity)
        }
    }

    fun requestDeleteRoom(roomId: String, onComplete: (Boolean) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference
        val requestRef = database.child("deleteRoomRequests").push()

        val requestData = mapOf(
            "roomId" to roomId
        )

        requestRef.setValue(requestData).addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }
}
