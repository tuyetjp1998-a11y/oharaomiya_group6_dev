<%-- ログイン画面 --%>
<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, initial-scale=1">

    <title>ログイン | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="a">
    <c:url var="loginExecuteUrl"
           value="/safety/auth/LoginExecute.action" />

    <main class="login-layout">
        <section class="login-story">
            <h1 class="login-brand">AnpiLink</h1>

            <div class="story-copy">
                <h3 class="story-heading">学校安否確認</h3>
                <h2>学校と家庭を、<br>安心でつなぐ。</h2>

                <div class="school-art" aria-hidden="true">
                    <div class="art-orbit"></div>
                    <div class="art-orbit second"></div>

                    <div class="art-school">
                        <svg viewBox="0 0 24 24"
                             fill="none"
                             stroke="currentColor"
                             stroke-width="1.7"
                             stroke-linecap="round"
                             stroke-linejoin="round">
                            <path d="m3 8 9-5 9 5v13H3ZM9 21v-7h6v7M12 6v4M10 8h4M5 12h2m10 0h2M5 16h2m10 0h2"/>
                        </svg>
                    </div>

                    <span class="art-check">
                        <svg viewBox="0 0 24 24"
                             fill="none"
                             stroke="currentColor"
                             stroke-width="1.7"
                             stroke-linecap="round"
                             stroke-linejoin="round">
                            <path d="m5 12 4 4L19 6"/>
                        </svg>
                    </span>
                </div>
            </div>
        </section>

        <section class="login-form-area">
            <div class="login-form">
                <h1 class="login-small-mark login-brand">
                    AnpiLink
                </h1>

                <h2>ログイン</h2>

                <p class="form-intro">
                    アカウント情報を入力してください
                </p>

                <c:if test="${requestScope.loginFailed}">
                    <p class="login-error" role="alert">
                        ユーザーIDまたはパスワードが正しくないか、
                        アカウントが利用できません。
                    </p>
                </c:if>

                <form action="${loginExecuteUrl}" method="post">
                    <input type="hidden"
                           name="csrfToken"
                           value="${sessionScope.csrfToken}">

                    <label for="user">ユーザーID</label>
                    <input id="user"
                           name="loginId"
                           type="text"
                           required
                           maxlength="100"
                           placeholder="ユーザーIDを入力"
                           autocomplete="username">

                    <label for="password">パスワード</label>
                    <input id="password"
                           name="password"
                           type="password"
                           required
                           autocomplete="current-password"
                           placeholder="パスワードを入力">

                    <button type="submit" class="login-button">
                        ログイン
                        <svg viewBox="0 0 24 24"
                             aria-hidden="true"
                             fill="none"
                             stroke="currentColor"
                             stroke-width="1.7"
                             stroke-linecap="round"
                             stroke-linejoin="round">
                            <path d="M5 12h14m-5-5 5 5-5 5"/>
                        </svg>
                    </button>
                </form>

                <%-- 新規登録機能の接続までは無効にする --%>
                <a class="register-link" aria-disabled="true">
                    新規登録（保護者）
                </a>
            </div>
        </section>
    </main>
</body>
</html>