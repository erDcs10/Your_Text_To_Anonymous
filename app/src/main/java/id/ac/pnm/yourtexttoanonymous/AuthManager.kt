package id.ac.pnm.yourtexttoanonymous

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun authenticate() {
        // TODO: Implement Google Sign-In via Credential Manager
    }
}
