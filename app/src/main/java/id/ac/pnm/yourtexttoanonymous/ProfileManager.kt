package id.ac.pnm.yourtexttoanonymous

import com.google.firebase.database.FirebaseDatabase

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
                onSuccess()
            }
    }
}
