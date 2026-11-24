package com.example.anpilink

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * 未送信の回答。
 * 再送時も送信ID・回答内容・位置情報を変更しない。
 */
data class PendingSafetyReport(
    val serverBaseUrl: String,
    val userId: Long,
    val requestId: String,
    val status: String,
    val dangerType: String?,
    val comment: String?,
    val location: StudentReportLocation?,
    val savedAt: String,
    val attempted: Boolean = true
)

/**
 * 利用者・接続先ごとに未送信回答を1件保存する。
 * アプリ専用のバックアップ対象外領域を使用する。
 * 認証トークンは保存しない。
 */
object PendingSafetyReportStore {

    private const val MAX_FILE_BYTES = 65_536

    private val lock = Any()

    private val dangerTypes = setOf(
        "ACCIDENT",
        "EARTHQUAKE",
        "FIRE",
        "LANDSLIDE",
        "STALKING"
    )

    private val worriedTypes = setOf(
        "ACCIDENT",
        "EARTHQUAKE",
        "FIRE",
        "LANDSLIDE",
        "HEATSTROKE"
    )

    /**
     * 未送信回答を取得する。
     * 読み取りエラーを「未送信なし」として扱わない。
     */
    suspend fun load(
        context: Context,
        userId: Long
    ): PendingSafetyReport? = withContext(Dispatchers.IO) {
        synchronized(lock) {
            read(context.applicationContext, userId)
        }
    }

    /**
     * 未送信回答がない場合だけ保存する。
     * 既存の回答を黙って上書きしない。
     */
    suspend fun create(
        context: Context,
        userId: Long,
        status: String,
        dangerType: String? = null,
        comment: String? = null,
        location: StudentReportLocation? = null,
        replacementRequestId: String? = null
    ): PendingSafetyReport = withContext(Dispatchers.IO) {
        synchronized(lock) {
            require(userId > 0) {
                "利用者IDが正しくありません。"
            }

            val normalizedComment = comment
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            val valid = when (status) {
                "SAFE" ->
                    dangerType == null &&
                            normalizedComment == null

                "DANGER" ->
                    (dangerType == null ||
                            dangerType in dangerTypes) &&
                            normalizedComment == null

                "WORRIED" ->
                    (dangerType == null ||
                            dangerType in worriedTypes) &&
                            (dangerType != null ||
                                    normalizedComment != null)

                else -> false
            }

            require(valid) {
                "回答内容を確認してください。"
            }

            require(
                normalizedComment?.contains('\u0000') != true
            ) {
                "コメントに使用できない文字が含まれています。"
            }

            val appContext = context.applicationContext

            val current = read(appContext, userId)
            if (replacementRequestId != null) {
                if (current == null || current.requestId != replacementRequestId || current.attempted) {
                    throw IOException("元の回答の状態が変わりました。通信状態画面で確認してください。")
                }
            } else if (current != null) {
                throw IOException(
                    "未送信の回答があります。" +
                            "通信状態画面で確認してください。"
                )
            }

            val pending = PendingSafetyReport(
                serverBaseUrl = ApiConfig.BASE_URL,
                userId = userId,
                requestId = UUID.randomUUID().toString(),
                status = status,
                dangerType = dangerType,
                comment = normalizedComment,
                location = location,
                savedAt = Instant.now().toString(),
                attempted = false
            )

            // APIの本文上限も保存前に確認する。
            val answer = JSONObject()
                .put("requestId", pending.requestId)
                .put("status", pending.status)
                .put(
                    "dangerType",
                    pending.dangerType ?: JSONObject.NULL
                )
                .put(
                    "comment",
                    pending.comment ?: JSONObject.NULL
                )

            if (location != null) {
                answer
                    .put("latitude", location.latitude)
                    .put("longitude", location.longitude)
                    .put(
                        "locationAccuracy",
                        location.locationAccuracy
                    )
                    .put(
                        "locationAcquiredAt",
                        location.locationAcquiredAt
                    )
            }

            require(answer.toString().length <= 8192) {
                "送信内容が長すぎます。" +
                        "コメントを短くしてください。"
            }

            val document = JSONObject()
                .put("version", 1)
                .put("attempted", false)
                .put("serverBaseUrl", pending.serverBaseUrl)
                .put("userId", pending.userId)
                .put("savedAt", pending.savedAt)
                .put("answer", answer)

            val bytes = document.toString()
                .toByteArray(Charsets.UTF_8)

            require(bytes.size <= MAX_FILE_BYTES) {
                "保存する回答が大きすぎます。"
            }

            val file = storageFile(appContext, userId)
            val output = file.startWrite()

            try {
                output.write(bytes)
                file.finishWrite(output)

            } catch (e: Exception) {
                try {
                    file.failWrite(output)
                } catch (rollbackError: Exception) {
                    e.addSuppressed(rollbackError)
                }

                throw e
            }

            pending
        }
    }

    /**
     * HTTP送信前に記録する。旧形式の回答は送信済みの可能性があるものとして扱う。
     * 置き換えと同じロックを使い、確認したIDが変わっていたら送信しない。
     */
    suspend fun markAttempted(
        context: Context,
        userId: Long,
        requestId: String
    ): Boolean = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val appContext = context.applicationContext
            val current = read(appContext, userId)
            if (current == null || current.requestId != requestId) {
                false
            } else {
                if (!current.attempted) {
                    val file = storageFile(appContext, userId)
                    // 元のJSONを保ち、回答本文・座標・UUIDを変更しない。
                    val document = file.openRead().use {
                        JSONObject(it.bufferedReader(Charsets.UTF_8).readText())
                    }.put("attempted", true)
                    val output = file.startWrite()
                    try {
                        output.write(document.toString().toByteArray(Charsets.UTF_8))
                        file.finishWrite(output)
                    } catch (e: Exception) {
                        file.failWrite(output)
                        throw e
                    }
                }
                true
            }
        }
    }

    /**
     * 指定した送信IDと一致する回答だけ削除する。
     * サーバー受付の確認後など、呼び出し側が判断して使用する。
     */
    suspend fun remove(
        context: Context,
        userId: Long,
        requestId: String
    ): Boolean = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val appContext = context.applicationContext
            val current = read(appContext, userId)

            if (current == null ||
                current.requestId != requestId
            ) {
                false

            } else {
                val file = storageFile(appContext, userId)
                file.delete()

                if (storageExists(file)) {
                    throw IOException(
                        "送信済み回答の一時保存を削除できませんでした。"
                    )
                }

                true
            }
        }
    }

    /**
     * 接続先と利用者IDから保存先を作成する。
     */
    private fun storageFile(
        context: Context,
        userId: Long
    ): AtomicFile {
        require(userId > 0) {
            "利用者IDが正しくありません。"
        }

        // ローカル環境とAWS環境の回答を混在させない。
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(
                ApiConfig.BASE_URL.toByteArray(
                    Charsets.UTF_8
                )
            )

        val serverKey = digest.joinToString("") {
            (it.toInt() and 0xff)
                .toString(16)
                .padStart(2, '0')
        }

        return AtomicFile(
            File(
                context.noBackupFilesDir,
                "pending_safety_${serverKey}_${userId}.json"
            )
        )
    }

    /**
     * 本体または復旧用ファイルの存在を確認する。
     * AtomicFile自体にはexists()がない。
     */
    private fun storageExists(
        file: AtomicFile
    ): Boolean {
        val base = file.baseFile

        return base.exists() ||
                File(base.path + ".bak").exists()
    }

    /**
     * 保存済みデータを読み取る。
     * 呼び出し元でlockを取得して使用する。
     */
    private fun read(
        context: Context,
        userId: Long
    ): PendingSafetyReport? {
        val file = storageFile(context, userId)

        if (!storageExists(file)) {
            return null
        }

        val bytes = file.openRead().use { input ->
            val buffer = ByteArray(MAX_FILE_BYTES + 1)
            var length = 0

            while (length < buffer.size) {
                val count = input.read(
                    buffer,
                    length,
                    buffer.size - length
                )

                if (count == -1) {
                    break
                }

                length += count
            }

            if (length > MAX_FILE_BYTES) {
                throw IOException(
                    "一時保存データが大きすぎます。"
                )
            }

            buffer.copyOf(length)
        }

        try {
            val document = JSONObject(
                String(bytes, Charsets.UTF_8)
            )

            if (document.getInt("version") != 1 ||
                document.getLong("userId") != userId ||
                document.getString("serverBaseUrl") !=
                ApiConfig.BASE_URL
            ) {
                throw IOException(
                    "一時保存データの利用者または接続先が一致しません。"
                )
            }

            val answer = document.getJSONObject("answer")

            val location = if (
                !answer.isNull("latitude") &&
                !answer.isNull("longitude") &&
                !answer.isNull("locationAccuracy") &&
                !answer.isNull("locationAcquiredAt")
            ) {
                StudentReportLocation(
                    latitude = answer.getDouble("latitude"),
                    longitude = answer.getDouble("longitude"),
                    locationAccuracy = answer.getDouble(
                        "locationAccuracy"
                    ),
                    locationAcquiredAt = answer.getString(
                        "locationAcquiredAt"
                    )
                )
            } else {
                null
            }

            return PendingSafetyReport(
                serverBaseUrl = document.getString(
                    "serverBaseUrl"
                ),
                userId = document.getLong("userId"),
                requestId = answer.getString("requestId"),
                status = answer.getString("status"),
                dangerType = if (
                    answer.isNull("dangerType")
                ) {
                    null
                } else {
                    answer.getString("dangerType")
                },
                comment = if (
                    answer.isNull("comment")
                ) {
                    null
                } else {
                    answer.getString("comment")
                },
                location = location,
                savedAt = document.getString("savedAt"),
                attempted = document.optBoolean("attempted", true)
            )

        } catch (e: JSONException) {
            throw IOException(
                "一時保存した回答を読み取れませんでした。",
                e
            )
        }
    }
}