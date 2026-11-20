package com.nowstation.app.guide

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/** 처음 실행 가이드를 끝까지(또는 건너뛰기로) 봤는지. 한 번 보면 다시 띄우지 않는다. */
@Singleton
class GuideStore @Inject constructor(private val prefs: SharedPreferences) {
    val seen: Boolean get() = prefs.getBoolean(KEY, false)

    fun markSeen() {
        prefs.edit().putBoolean(KEY, true).apply()
    }

    private companion object {
        const val KEY = "guide_seen_v1"
    }
}
