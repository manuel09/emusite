package com.emusite.app.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Details : Screen("details/{sourceId}/{url}") {
        fun createRoute(sourceId: String, url: String): String =
            "details/$sourceId/$url"
    }
    data object Episodes : Screen("episodes/{sourceId}/{url}/{title}") {
        fun createRoute(sourceId: String, url: String, title: String): String =
            "episodes/$sourceId/$url/$title"
    }
    data object Player : Screen("player/{sourceId}/{url}/{title}") {
        fun createRoute(sourceId: String, url: String, title: String): String =
            "player/$sourceId/$url/$title"
    }
    data object Plugins : Screen("plugins")
    data object Settings : Screen("settings")
    data object RepoDetail : Screen("repo/{repoUrl}") {
        fun createRoute(repoUrl: String): String = "repo/$repoUrl"
    }
    data object BrowseSection : Screen("browse/{sectionIndex}") {
        fun createRoute(sectionIndex: Int): String = "browse/$sectionIndex"
    }
}
