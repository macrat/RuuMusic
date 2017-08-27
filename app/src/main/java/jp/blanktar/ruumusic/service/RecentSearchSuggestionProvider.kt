package jp.blanktar.ruumusic.service


import android.content.SearchRecentSuggestionsProvider


class RecentSearchSuggestionProvider : SearchRecentSuggestionsProvider() {
    init {
        setupSuggestions(AUTHORITY, MODE)
    }

    companion object {
        const val AUTHORITY = "jp.blanktar.ruumusic.RecentSearchSuggestionProvider"
        const val MODE = DATABASE_MODE_QUERIES
    }
}
