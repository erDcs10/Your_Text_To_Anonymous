package id.ac.pnm.yourtexttoanonymous.data.model

// Keep the default values empty/standard so Firebase can deserialize it automatically
data class UserProfile(
    val displayName: String = "",
    val gender: String = "Male"
)