package com.example.anpilink

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * リフレッシュトークンを暗号化して保存する。
 * パスワードとアクセストークンは保存しない。
 */
class TokenStore(context: Context) {

    private val tokenFile = AtomicFile(
        File(
            context.applicationContext.noBackupFilesDir,
            "anpilink_refresh_token.bin"
        )
    )

    /**
     * リフレッシュトークンを保存する。
     */
    suspend fun save(refreshToken: String): Unit =
        withContext(Dispatchers.IO) {
            storageMutex.withLock {

                if (!TOKEN_PATTERN.matches(refreshToken)) {
                    throw IOException(
                        "保存する認証情報の形式が正しくありません。"
                    )
                }

                val encrypted = protect {
                    val cipher = Cipher.getInstance(TRANSFORMATION)

                    cipher.init(
                        Cipher.ENCRYPT_MODE,
                        getKey(createIfMissing = true)
                    )

                    val iv = cipher.iv

                    if (iv.size != IV_SIZE) {
                        throw IOException(
                            "暗号化の初期化に失敗しました。"
                        )
                    }

                    val ciphertext = cipher.doFinal(
                        refreshToken.toByteArray(Charsets.UTF_8)
                    )

                    // 形式バージョン + IV + 暗号化データ
                    byteArrayOf(FORMAT_VERSION) + iv + ciphertext
                }

                val output = tokenFile.startWrite()

                try {
                    output.write(encrypted)
                    tokenFile.finishWrite(output)

                } catch (e: Throwable) {
                    tokenFile.failWrite(output)
                    throw e
                }
            }
        }

    /**
     * 保存済みのリフレッシュトークンを取得する。
     * 未保存の場合はnullを返す。
     */
    suspend fun load(): String? =
        withContext(Dispatchers.IO) {
            storageMutex.withLock {

                val input = try {
                    tokenFile.openRead()

                } catch (e: FileNotFoundException) {
                    if (!tokenFile.baseFile.exists()) {
                        return@withLock null
                    }

                    throw e
                }

                // 正常な保存データは72バイト。過大な読み込みを防ぐ
                val payload = input.use { stream ->
                    val buffer = ByteArray(PAYLOAD_SIZE + 1)
                    var length = 0

                    while (length < buffer.size) {
                        val count = stream.read(
                            buffer,
                            length,
                            buffer.size - length
                        )

                        if (count == -1) {
                            break
                        }

                        length += count
                    }

                    buffer.copyOf(length)
                }

                if (payload.size != PAYLOAD_SIZE
                    || payload[0] != FORMAT_VERSION
                ) {
                    throw IOException(
                        "保存済み認証情報の形式が正しくありません。"
                    )
                }

                val token = protect {
                    val iv = payload.copyOfRange(
                        1,
                        1 + IV_SIZE
                    )

                    val ciphertext = payload.copyOfRange(
                        1 + IV_SIZE,
                        payload.size
                    )

                    val cipher = Cipher.getInstance(TRANSFORMATION)

                    cipher.init(
                        Cipher.DECRYPT_MODE,
                        getKey(createIfMissing = false),
                        GCMParameterSpec(128, iv)
                    )

                    String(
                        cipher.doFinal(ciphertext),
                        Charsets.UTF_8
                    )
                }

                if (!TOKEN_PATTERN.matches(token)) {
                    throw IOException(
                        "保存済み認証情報を確認できませんでした。"
                    )
                }

                token
            }
        }

    /**
     * 端末に保存した認証情報を削除する。
     */
    suspend fun clear(): Unit =
        withContext(Dispatchers.IO) {
            storageMutex.withLock {
                tokenFile.delete()

                if (tokenFile.baseFile.exists()) {
                    throw IOException(
                        "保存済み認証情報を削除できませんでした。"
                    )
                }
            }
        }

    /**
     * Android Keystore内の鍵を取得する。
     */
    private fun getKey(createIfMissing: Boolean): SecretKey {

        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        val existing = keyStore.getKey(KEY_ALIAS, null)

        if (existing is SecretKey) {
            return existing
        }

        if (!createIfMissing) {
            throw IOException(
                "保存済み認証情報の暗号鍵が見つかりません。"
            )
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setRandomizedEncryptionRequired(true)
                .build()
        )

        return generator.generateKey()
    }

    /**
     * 暗号処理の失敗をIOExceptionとして通知する。
     */
    private fun <T> protect(operation: () -> T): T {
        return try {
            operation()
        } catch (e: GeneralSecurityException) {
            throw IOException(
                "認証情報の暗号処理に失敗しました。",
                e
            )
        }
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "anpilink_refresh_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        private const val FORMAT_VERSION: Byte = 1
        private const val IV_SIZE = 12

        // バージョン1 + IV12 + トークン43 + 認証タグ16
        private const val PAYLOAD_SIZE = 72

        private val TOKEN_PATTERN =
            Regex("[A-Za-z0-9_-]{43}")

        // 複数のTokenStoreから同時に操作されることを防ぐ
        private val storageMutex = Mutex()
    }
}