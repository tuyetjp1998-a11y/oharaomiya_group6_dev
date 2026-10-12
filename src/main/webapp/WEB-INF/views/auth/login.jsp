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

    <title>ログイン | 学校安否確認</title>

    <link rel="stylesheet"
      href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <div class="login">
        <section class="login-art">
            <h1>学校と家庭をつなぐ<br>安否確認</h1>
        </section>

        <main class="login-box">
            <div class="brand">学校安否確認</div>
            <h1>ログイン</h1>

            <%-- 認証に失敗した場合 --%>
            <c:if test="${requestScope.loginFailed}">
                <p class="form-error" role="alert">
                    ユーザーID、パスワード、または利用者区分が正しくありません。
                </p>
            </c:if>

            <c:url var="loginExecuteUrl"
                   value="/safety/auth/LoginExecute.action"/>

            <form class="login-form"
                  action="${loginExecuteUrl}"
                  method="post">

                <%-- LoginActionで生成したCSRFトークン --%>
                <input type="hidden"
                       name="csrfToken"
                       value="${sessionScope.csrfToken}">

                <label for="loginId">ユーザーID</label>
                <input type="text"
                       id="loginId"
                       name="loginId"
                       autocomplete="username"
                       maxlength="100"
                       required>

                <label for="password">パスワード</label>
                <input type="password"
                       id="password"
                       name="password"
                       autocomplete="current-password"
                       required>

                <label for="role">利用者</label>
                <select id="role" name="role" required>
                    <option value="STUDENT">生徒</option>
                    <option value="FAMILY">保護者</option>
                    <option value="TEACHER">教員</option>
                    <option value="PRINCIPAL">校長先生</option>
                </select>

                <button type="submit" class="primary">
                    ログイン
                </button>
            </form>
            <c:url var="registerUrl" value="/safety/auth/Register.action" />

			<a class="primary register-link" href="${registerUrl}">
    			新規登録
			</a>
        </main>
    </div>
</body>
</html>