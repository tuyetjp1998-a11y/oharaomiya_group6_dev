package com.example.anpilink

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 利用者・接続先ごとに設定を保存する。座標や認証情報は保存しない。
 * 未確認の設定をONとして扱わない。
 */
object StudentSettingsCache {
    private val lock = Any()

    private fun preferences(context: Context, userId: Long): SharedPreferences {
        require(userId > 0)
        val serverKey = MessageDigest.getInstance("SHA-256")
            .digest(ApiConfig.BASE_URL.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return context.applicationContext.getSharedPreferences(
            "student_settings_${serverKey}_$userId",
            Context.MODE_PRIVATE
        )
    }

    private fun commit(editor: SharedPreferences.Editor) {
        if (!editor.commit()) {
            throw IOException("端末に設定を保存できませんでした。")
        }
    }

    suspend fun load(context: Context, userId: Long): StudentSettingsResult? =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val prefs = preferences(context, userId)
                if (!prefs.contains("location") || !prefs.contains("notification")) {
                    null
                } else {
                    StudentSettingsResult(
                        locationEnabled = prefs.getBoolean("location", false),
                        notificationEnabled = prefs.getBoolean("notification", false),
                        fromCache = true,
                        locationOffPending = prefs.getBoolean("off_pending", false)
                    )
                }
            }
        }

    suspend fun locationEnabled(context: Context, userId: Long): Boolean? =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val prefs = preferences(context, userId)
                if (prefs.contains("location")) prefs.getBoolean("location", false) else null
            }
        }

    suspend fun offPending(context: Context, userId: Long): Boolean =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                preferences(context, userId).getBoolean("off_pending", false)
            }
        }

    suspend fun revision(context: Context, userId: Long): Long =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                preferences(context, userId).getLong("revision", 0L)
            }
        }

    /** 古いGET応答が、その後のスイッチ操作を上書きしないようにする。 */
    suspend fun rememberServerSettings(
        context: Context,
        userId: Long,
        expectedRevision: Long,
        settings: StudentSettingsResult
    ): StudentSettingsResult = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val prefs = preferences(context, userId)
            val changed = prefs.getLong("revision", 0L) != expectedRevision
            val pendingOff = prefs.getBoolean("off_pending", false)
            val effective = StudentSettingsResult(
                locationEnabled = when {
                    pendingOff -> false
                    changed && prefs.contains("location") -> prefs.getBoolean("location", false)
                    else -> settings.locationEnabled
                },
                notificationEnabled = if (changed && prefs.contains("notification")) {
                    prefs.getBoolean("notification", false)
                } else {
                    settings.notificationEnabled
                },
                fromCache = changed || pendingOff,
                locationOffPending = pendingOff
            )
            commit(prefs.edit()
                .putBoolean("location", effective.locationEnabled)
                .putBoolean("notification", effective.notificationEnabled))
            effective
        }
    }

    /** OFF操作は通信前に反映し、通信失敗後も端末ではOFFを維持する。 */
    suspend fun beginUpdate(
        context: Context,
        userId: Long,
        locationOff: Boolean
    ): Long = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val prefs = preferences(context, userId)
            val revision = prefs.getLong("revision", 0L) + 1L
            val editor = prefs.edit().putLong("revision", revision)
            if (locationOff) {
                editor.putBoolean("location", false).putBoolean("off_pending", true)
            }
            commit(editor)
            revision
        }
    }

    suspend fun confirmUpdate(
        context: Context,
        userId: Long,
        expectedRevision: Long,
        locationSetting: Boolean,
        enabled: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val prefs = preferences(context, userId)
            val key = if (locationSetting) "location" else "notification"
            if (prefs.getLong("revision", 0L) != expectedRevision) {
                prefs.getBoolean(key, false)
            } else {
                val editor = prefs.edit().putBoolean(key, enabled)
                if (locationSetting) editor.putBoolean("off_pending", false)
                commit(editor)
                enabled
            }
        }
    }

    /** ローカルサーバーも使うため、VALIDATEDだけを必須条件にしない。 */
    fun hasNetwork(context: Context): Boolean {
        return try {
            val manager = context.applicationContext.getSystemService(
                ConnectivityManager::class.java
            ) ?: return false
            val network = manager.activeNetwork ?: return false
            manager.getNetworkCapabilities(network)?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) == true
        } catch (e: SecurityException) {
            false
        }
    }
}
