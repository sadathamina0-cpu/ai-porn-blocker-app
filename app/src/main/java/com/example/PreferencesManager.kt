package com.example

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    private const val PREFS_NAME = "purity_lock_prefs"
    private const val KEY_PROTECTION_ENABLED = "protection_enabled"
    private const val KEY_BLOCKED_WORDS = "blocked_words"
    private const val KEY_CASE_INSENSITIVE = "case_insensitive"
    private const val KEY_REPLACEMENT_TEXT = "replacement_text"

    private val DEFAULT_BLOCKED_WORDS = setOf(
        "porn",
        "porno",
        "sexvideo",
        "xxx",
        "xvideos",
        "porn video",
        "x videos",
        "xvideos porn"
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PROTECTION_ENABLED, true)
    }

    fun setProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PROTECTION_ENABLED, enabled).apply()
    }

    fun getBlockedWords(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_BLOCKED_WORDS, null) ?: DEFAULT_BLOCKED_WORDS
    }

    fun setBlockedWords(context: Context, words: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_BLOCKED_WORDS, words).apply()
    }

    fun addBlockedWord(context: Context, word: String): Boolean {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return false
        val current = getBlockedWords(context).toMutableSet()
        val added = current.add(trimmed.lowercase())
        if (added) {
            setBlockedWords(context, current)
        }
        return added
    }

    fun removeBlockedWord(context: Context, word: String): Boolean {
        val current = getBlockedWords(context).toMutableSet()
        val removed = current.remove(word.lowercase())
        if (removed) {
            setBlockedWords(context, current)
        }
        return removed
    }

    fun isCaseInsensitive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CASE_INSENSITIVE, true)
    }

    fun setCaseInsensitive(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CASE_INSENSITIVE, enabled).apply()
    }

    fun getReplacementText(context: Context): String {
        return getPrefs(context).getString(KEY_REPLACEMENT_TEXT, "") ?: ""
    }

    fun setReplacementText(context: Context, text: String) {
        getPrefs(context).edit().putString(KEY_REPLACEMENT_TEXT, text).apply()
    }

    fun resetToDefaults(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_PROTECTION_ENABLED, true)
            .putStringSet(KEY_BLOCKED_WORDS, DEFAULT_BLOCKED_WORDS)
            .putBoolean(KEY_CASE_INSENSITIVE, true)
            .putString(KEY_REPLACEMENT_TEXT, "")
            .apply()
    }
}
