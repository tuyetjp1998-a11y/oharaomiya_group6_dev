package com.example.anpilink

/**
 * 保存済みトークンからログイン状態を復元する。
 */
object SessionRestorer {

    suspend fun restore(
        tokenStore: TokenStore
    ): LoginSession? {

        val refreshToken = tokenStore.load()
            ?: return null

        try {
            // 保存済みトークンで新しいアクセストークンを取得する
            val refreshed = RefreshApi.refresh(refreshToken)

            // 利用者情報は端末の保存値ではなくサーバーから取得する
            val user = MeApi.getUser(
                accessToken = refreshed.accessToken
            )

            return LoginSession(
                accessToken = refreshed.accessToken,
                refreshToken = refreshToken,
                accessExpiresAt = refreshed.accessExpiresAt,
                refreshExpiresAt = refreshed.refreshExpiresAt,
                userId = user.id,
                userName = user.name,
                role = user.role
            )

        } catch (e: RefreshApiException) {
            if (e.statusCode == 401) {
                tokenStore.clear()
            }

            throw e

        } catch (e: MeApiException) {
            if (e.statusCode == 401 || e.statusCode == 403) {
                tokenStore.clear()
            }

            throw e
        }
    }
}