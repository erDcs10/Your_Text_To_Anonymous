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

    fun listenForRevealRequests(
        roomId: String,
        onStrangerRequested: () -> Unit,
        onBothRevealed: () -> Unit // <-- New Callback!
    ) {
        val revealRef = db.child("rooms").child(roomId).child("revealRequests")

        revealRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var strangerRequested = false
                var iRequested = false

                // Check the status of both users in the room
                snapshot.children.forEach { child ->
                    if (child.key == currentUserId && (child.value as? Boolean) == true) {
                        iRequested = true
                    } else if (child.key != currentUserId && (child.value as? Boolean) == true) {
                        strangerRequested = true
                    }
                }

                // If both people said yes, trigger the handshake!
                if (strangerRequested && iRequested) {
                    onBothRevealed()
                } else if (strangerRequested) {
                    // Otherwise, just show the pink banner
                    onStrangerRequested()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // New function to save the room to the Inbox
    fun saveToPersistentRoom(roomId: String) {
        db.child("users").child(currentUserId).child("persistentRooms").child(roomId).setValue(true)
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

    fun getStrangerProfile(roomId: String, onResult: (name: String?, gender: String?) -> Unit) {
        android.util.Log.d("ChatManager", "Attempting to fetch stranger profile for room: $roomId")

        db.child("rooms").child(roomId).child("users").get().addOnSuccessListener { snapshot ->
            // Find the User ID in this room that is NOT our own ID
            val strangerUid = snapshot.children.mapNotNull { it.key }.firstOrNull { it != currentUserId }

            if (strangerUid != null) {
                android.util.Log.d("ChatManager", "Found stranger UID: $strangerUid. Fetching profile...")

                // Fetch their profile using the new Firebase Rule we just added
                db.child("users").child(strangerUid).child("profile").get()
                    .addOnSuccessListener { profileSnap ->
                        val name = profileSnap.child("displayName").getValue(String::class.java)
                        val gender = profileSnap.child("gender").getValue(String::class.java)

                        android.util.Log.d("ChatManager", "Profile fetched successfully! Name: $name, Gender: $gender")
                        onResult(name, gender)
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("ChatManager", "Failed to read stranger's profile. Rule blocked?", e)
                        onResult(null, null)
                    }
            } else {
                android.util.Log.e("ChatManager", "Stranger UID not found in room! Was this an old room?")
                onResult(null, null)
            }
        }.addOnFailureListener { e ->
            android.util.Log.e("ChatManager", "Failed to read room users list.", e)
            onResult(null, null)
        }
    }
}
