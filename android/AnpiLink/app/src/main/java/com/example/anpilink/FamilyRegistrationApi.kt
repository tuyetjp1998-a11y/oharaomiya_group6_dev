package com.example.anpilink

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

class FamilyRegistrationException(message: String) : IOException(message)

object FamilyRegistrationApi {

    private const val REGISTER_URL =
        ApiConfig.BASE_URL + "/api/auth/Register.action"

    suspend fun register(
        name: String,
        loginId: String,
        password: String,
        studentLoginId: String
    ): Unit = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("name", name.trim())
            .put("loginId", loginId.trim())
            .put("password", password)
            .put("studentLoginId", studentLoginId.trim())
            .toString()
            .toByteArray(Charsets.UTF_8)

        val connection = URL(REGISTER_URL)
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "Content-Type", "application/json; charset=UTF-8"
            )
            connection.setFixedLengthStreamingMode(payload.size)

            connection.outputStream.use { it.write(payload) }

            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_CREATED) {
                val errorBody = connection.errorStream
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }

                val serverMessage = try {
                    errorBody?.let { JSONObject(it).optString("message") }
                        ?.takeIf { it.isNotBlank() }
                } catch (e: JSONException) {
                    null
                }

                throw FamilyRegistrationException(
                    serverMessage ?: "登録できませんでした。（HTTP $status）"
                )
            }

            val response = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { JSONObject(it.readText()) }

            if (response.opt("registered") != true) {
                throw FamilyRegistrationException("登録結果を確認できませんでした。")
            }
        } finally {
            connection.disconnect()
        }
    }
}