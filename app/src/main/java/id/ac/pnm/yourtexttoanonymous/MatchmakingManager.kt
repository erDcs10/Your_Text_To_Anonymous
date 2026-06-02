package id.ac.pnm.yourtexttoanonymous

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

class MatchmakingManager {
    private val db = FirebaseDatabase.getInstance().reference

    fun joinQueue(uid: String, onMatchFound: (String) -> Unit) {
        val queueRef = db.child("queue").child(uid)

        queueRef.onDisconnect().removeValue()
        
        queueRef.setValue(mapOf("timestamp" to ServerValue.TIMESTAMP))
            .addOnSuccessListener {
                Log.d("Matchmaking", "Joined queue successfully")
                listenForMatch(uid, onMatchFound) 
            }
            .addOnFailureListener {
                Log.e("Matchmaking", "Failed to join queue", it)
            }
    }

    fun leaveQueue(userId: String){
      db.child("queue").child(userId).removeValue()
      db.child("queue").child(userId).onDisconnect().cancel()
    }

    private fun listenForMatch(uid: String, onMatchFound: (String) -> Unit) {
        val userRoomRef = db.child("users").child(uid).child("activeRoom")
        
        userRoomRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val roomId = snapshot.getValue(String::class.java)
                if (roomId != null) {
                    Log.d("Matchmaking", "Match found! Room ID: $roomId")
                    onMatchFound(roomId)
                    userRoomRef.removeEventListener(this) 
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Matchmaking", "Listen cancelled", error.toException())
            }
        })
    }
}
