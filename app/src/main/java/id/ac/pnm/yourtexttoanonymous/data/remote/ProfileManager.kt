package id.ac.pnm.yourtexttoanonymous.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import id.ac.pnm.yourtexttoanonymous.data.model.UserProfile

class ProfileManager {
    private val db = FirebaseDatabase.getInstance().reference

    fun checkProfileExists(uid: String, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("profile").get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.exists())
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun saveProfile(uid: String, displayName: String, gender: String, onSuccess: () -> Unit) {
        val profileData = mapOf(
            "displayName" to displayName,
            "gender" to gender
        )
        db.child("users").child(uid).child("profile").setValue(profileData)
            .addOnSuccessListener {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        db.child("users").child(uid).child("fcmToken").setValue(task.result)
                    }
                    onSuccess()
                }
            }
    }

    fun getProfile(uid: String, onResult: (UserProfile?) -> Unit) {
        db.child("users").child(uid).child("profile").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val profile = snapshot.getValue(UserProfile::class.java)
                    onResult(profile)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}