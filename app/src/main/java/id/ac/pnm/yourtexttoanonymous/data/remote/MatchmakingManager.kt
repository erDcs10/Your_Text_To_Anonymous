package id.ac.pnm.yourtexttoanonymous.data.remote

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class MatchmakingManager {
    private val db = FirebaseDatabase.getInstance().reference

    fun joinQueue(uid: String, onMatchFound: (String) -> Unit) {
        val queueRef = db.child("queue")

        // GUEST PHASE: Look for a partner
        queueRef.limitToFirst(2).get()
            .addOnSuccessListener { snapshot ->
                val partnerNode = snapshot.children.firstOrNull { it.key != uid }

                if (partnerNode != null) {
                    // 1. WE FOUND A PARTNER!
                    val partnerUid = partnerNode.key!!
                    val roomId = "anon_${UUID.randomUUID().toString().take(8)}"

                    // 2. CRITICAL STEP: Build the room and explicitly list both users inside it!
                    val initialRoomData = mapOf(
                        "users" to mapOf(
                            uid to true,         // Put our ID in the room
                            partnerUid to true   // Put their ID in the room
                        ),
                        "createdAt" to ServerValue.TIMESTAMP,
                        "status" to "active"
                    )

                    // 3. Save this perfect room to the database FIRST
                    db.child("rooms").child(roomId).setValue(initialRoomData)
                        .addOnSuccessListener {
                            // 4. ONLY AFTER the room is built, connect the users
                            queueRef.child(partnerUid).removeValue()
                            db.child("users").child(partnerUid).child("activeRoom").setValue(roomId)

                            Log.d("Matchmaking", "Room completely built! Joined room: $roomId")
                            onMatchFound(roomId)
                        }
                        .addOnFailureListener { e ->
                            Log.e("Matchmaking", "Failed to build room structure", e)
                        }

                } else {
                    // HOST PHASE: Nobody is waiting, put ourselves in the queue
                    queueRef.child(uid).setValue(mapOf("timestamp" to ServerValue.TIMESTAMP))
                        .addOnSuccessListener {
                            queueRef.child(uid).onDisconnect().removeValue()
                            listenForMatch(uid, onMatchFound)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Matchmaking", "Failed to check queue", e)
            }
    }

    fun leaveQueue(userId: String) {
        db.child("queue").child(userId).removeValue()
        db.child("queue").child(userId).onDisconnect().cancel()
        db.child("users").child(userId).child("activeRoom").removeValue()
    }

    private fun listenForMatch(uid: String, onMatchFound: (String) -> Unit) {
        val userRoomRef = db.child("users").child(uid).child("activeRoom")

        userRoomRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val roomId = snapshot.getValue(String::class.java)

                if (roomId != null) {
                    userRoomRef.removeEventListener(this)
                    userRoomRef.removeValue()
                    onMatchFound(roomId)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}