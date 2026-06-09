package id.ac.pnm.yourtexttoanonymous.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ProfileSetup : Screen("profile_setup")
    object Inbox : Screen("inbox")

    // Dynamic route that accepts arguments
    object Chat : Screen("chat/{roomId}/{isAnonymous}") {
        fun createRoute(roomId: String, isAnonymous: Boolean): String {
            return "chat/$roomId/$isAnonymous"
        }
    }
}