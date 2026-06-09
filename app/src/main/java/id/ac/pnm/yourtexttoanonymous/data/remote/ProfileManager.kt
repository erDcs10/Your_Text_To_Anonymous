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

    // 2. ADD THIS GET PROFILE FUNCTION
    fun getProfile(uid: String, onResult: (UserProfile?) -> Unit) {
        // We target the exact "profile" node where the data is saved
        db.child("users").child(uid).child("profile").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Firebase automatically converts the JSON into your UserProfile data class
                    val profile = snapshot.getValue(UserProfile::class.java)
                    onResult(profile)
                } else {
                    onResult(null) // Data doesn't exist
                }
            }
            .addOnFailureListener {
                onResult(null) // Request failed (e.g., no internet)
            }
    }
}