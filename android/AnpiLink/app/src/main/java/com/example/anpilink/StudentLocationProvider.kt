package com.example.anpilink

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import java.time.Instant
import java.time.ZoneOffset
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 安否回答に添付する位置情報。
 */
data class StudentReportLocation(
    val latitude: Double,
    val longitude: Double,
    val locationAccuracy: Double,
    val locationAcquiredAt: String
)

/**
 * 利用設定と端末の権限を確認し、位置情報を1回取得する。
 * 診断ログには座標を出力しない。
 */
object StudentLocationProvider {

    private const val TAG = "AnpiLinkLocation"

    private const val GPS_TIMEOUT_MS = 10_000L
    private const val NETWORK_TIMEOUT_MS = 4_000L
    private const val MAX_LOCATION_AGE_MS = 60_000L

    /**
     * タイムアウトと、取得結果がnullの場合を区別する。
     */
    private data class LocationResponse(
        val location: Location?
    )

    suspend fun getOnce(
        context: Context,
        locationEnabled: Boolean
    ): StudentReportLocation? =
        withContext(Dispatchers.Main.immediate) {

            val startedAt = SystemClock.elapsedRealtime()

            Log.d(TAG, "端末の位置情報取得を開始")

            if (!locationEnabled) {
                Log.d(TAG, "取得中止：アプリの位置情報利用設定がOFF")
                return@withContext null
            }

            val appContext = context.applicationContext

            val fineGranted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarseGranted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            Log.d(
                TAG,
                "位置情報権限：fine=$fineGranted, coarse=$coarseGranted"
            )

            if (!fineGranted && !coarseGranted) {
                Log.w(TAG, "取得中止：位置情報の権限なし")
                return@withContext null
            }

            val manager = appContext.getSystemService(
                LocationManager::class.java
            )

            if (manager == null) {
                Log.w(TAG, "取得中止：LocationManagerを取得できません")
                return@withContext null
            }

            try {
                if (!LocationManagerCompat.isLocationEnabled(manager)) {
                    Log.w(TAG, "取得中止：端末の位置情報設定がOFF")
                    return@withContext null
                }

                // 概算位置のみ許可されている場合はGPSを使用しない。
                val providers = if (fineGranted) {
                    listOf(
                        LocationManager.GPS_PROVIDER,
                        LocationManager.NETWORK_PROVIDER
                    )
                } else {
                    listOf(LocationManager.NETWORK_PROVIDER)
                }

                var requestedCount = 0

                for (provider in providers) {
                    if (!manager.allProviders.contains(provider)) {
                        Log.d(
                            TAG,
                            "provider=$provider：端末に存在しないためスキップ"
                        )
                        continue
                    }

                    if (!manager.isProviderEnabled(provider)) {
                        Log.d(
                            TAG,
                            "provider=$provider：無効のためスキップ"
                        )
                        continue
                    }

                    requestedCount++
                    val timeoutMs = if (provider == LocationManager.GPS_PROVIDER) {
                        GPS_TIMEOUT_MS
                    } else {
                        NETWORK_TIMEOUT_MS
                    }

                    val requestStartedAt =
                        SystemClock.elapsedRealtime()

                    Log.d(
                        TAG,
                        "provider=$provider：取得要求開始、timeoutMs=$timeoutMs"
                    )

                    val response = withTimeoutOrNull(timeoutMs) {
                        LocationResponse(
                            requestLocation(
                                context = appContext,
                                manager = manager,
                                provider = provider
                            )
                        )
                    }

                    val waitedMs =
                        SystemClock.elapsedRealtime() -
                                requestStartedAt

                    if (response == null) {
                        Log.w(
                            TAG,
                            "provider=$provider：タイムアウト、" +
                                    "waitedMs=$waitedMs"
                        )
                        continue
                    }

                    val location = response.location

                    if (location == null) {
                        Log.w(
                            TAG,
                            "provider=$provider：取得結果がnull、" +
                                    "waitedMs=$waitedMs"
                        )
                        continue
                    }

                    Log.d(
                        TAG,
                        "provider=$provider：位置情報を受信、" +
                                "waitedMs=$waitedMs"
                    )

                    val result = toReportLocation(
                        location = location,
                        provider = provider
                    )

                    if (result != null) {
                        val totalMs =
                            SystemClock.elapsedRealtime() -
                                    startedAt

                        Log.d(
                            TAG,
                            "位置情報取得成功：provider=$provider、" +
                                    "totalMs=$totalMs"
                        )

                        return@withContext result
                    }
                }

                val totalMs =
                    SystemClock.elapsedRealtime() - startedAt

                Log.w(
                    TAG,
                    "位置情報取得終了：有効な位置情報なし、" +
                            "requestedProviders=$requestedCount、" +
                            "totalMs=$totalMs"
                )

                null

            } catch (e: CancellationException) {
                // 呼び出し元のキャンセルは取得失敗として握りつぶさない。
                Log.d(TAG, "位置情報取得中断：呼び出し元からキャンセル")
                throw e

            } catch (e: SecurityException) {
                Log.w(
                    TAG,
                    "位置情報取得失敗：SecurityException、" +
                            "取得直前の権限変更などを確認"
                )
                null

            } catch (e: IllegalArgumentException) {
                Log.w(
                    TAG,
                    "位置情報取得失敗：IllegalArgumentException、" +
                            "providerの対応状況などを確認"
                )
                null
            }
        }

    /**
     * タイムアウト・呼び出し元のキャンセル時は取得処理も中止する。
     */
    @SuppressLint("MissingPermission")
    private suspend fun requestLocation(
        context: Context,
        manager: LocationManager,
        provider: String
    ): Location? = suspendCancellableCoroutine { continuation ->

        val cancellationSignal = CancellationSignal()

        continuation.invokeOnCancellation {
            cancellationSignal.cancel()
        }

        LocationManagerCompat.getCurrentLocation(
            manager,
            provider,
            cancellationSignal,
            ContextCompat.getMainExecutor(context)
        ) { location ->
            if (continuation.isActive) {
                continuation.resume(location)
            }
        }
    }

    /**
     * 古い位置情報や不正な値は送信対象にしない。
     * 除外理由だけ記録し、座標・取得日時の値は記録しない。
     */
    private fun toReportLocation(
        location: Location,
        provider: String
    ): StudentReportLocation? {

        val ageMs = (
                SystemClock.elapsedRealtimeNanos() -
                        location.elapsedRealtimeNanos
                ) / 1_000_000L

        val latitude = location.latitude
        val longitude = location.longitude
        val accuracy = location.accuracy.toDouble()

        val rejectionReason = when {
            ageMs < 0 ->
                "位置情報の経過時間が負の値"

            ageMs > MAX_LOCATION_AGE_MS ->
                "位置情報が古い"

            !location.hasAccuracy() ->
                "精度情報なし"

            !latitude.isFinite() ->
                "緯度が有限の数値ではない"

            !longitude.isFinite() ->
                "経度が有限の数値ではない"

            !accuracy.isFinite() ->
                "精度が有限の数値ではない"

            latitude !in -90.0..90.0 ->
                "緯度が範囲外"

            longitude !in -180.0..180.0 ->
                "経度が範囲外"

            accuracy < 0 ->
                "精度が負の値"

            location.time <= 0 ->
                "取得日時が無効"

            else -> null
        }

        if (rejectionReason != null) {
            Log.w(
                TAG,
                "provider=$provider：受信した位置情報を除外、" +
                        "reason=$rejectionReason、ageMs=$ageMs"
            )
            return null
        }

        Log.d(
            TAG,
            "provider=$provider：位置情報の検証成功、ageMs=$ageMs"
        )

        return StudentReportLocation(
            latitude = latitude,
            longitude = longitude,
            locationAccuracy = accuracy,
            locationAcquiredAt = Instant
                .ofEpochMilli(location.time)
                .atOffset(ZoneOffset.UTC)
                .toString()
        )
    }
}