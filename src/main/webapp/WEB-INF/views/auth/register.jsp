<%-- 新規登録画面 --%>
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

    <title>新規登録 | 学校安否確認</title>

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
            <h1>新規登録</h1>

            <p>
                生徒または保護者のアカウントを登録します。
            </p>

            <%-- 登録に失敗した場合 --%>
            <c:if test="${not empty requestScope.registerError}">
                <p class="form-error" role="alert">
                    <c:out value="${requestScope.registerError}" />
                </p>
            </c:if>

            <c:url var="registerExecuteUrl"
                   value="/safety/auth/RegisterExecute.action" />

            <form class="login-form"
                  action="${registerExecuteUrl}"
                  method="post">

                <%-- CSRF対策 --%>
                <input type="hidden"
                       name="csrfToken"
                       value="${sessionScope.csrfToken}">

                <label for="role">利用者区分</label>
                <select id="role" name="role" required>
                    <option value="">選択してください</option>
                    <option value="STUDENT"
                            ${param.role eq 'STUDENT'
                              ? 'selected' : ''}>
                        生徒
                    </option>
                    <option value="FAMILY"
                            ${param.role eq 'FAMILY'
                              ? 'selected' : ''}>
                        保護者
                    </option>
                </select>

                <label for="name">氏名</label>
                <input type="text"
                       id="name"
                       name="name"
                       value="<c:out value='${param.name}' />"
                       autocomplete="name"
                       maxlength="100"
                       required>

                <label for="loginId">ユーザーID</label>
                <input type="text"
                       id="loginId"
                       name="loginId"
                       value="<c:out value='${param.loginId}' />"
                       autocomplete="username"
                       minlength="4"
                       maxlength="100"
                       pattern="[A-Za-z0-9_-]{4,100}"
                       aria-describedby="loginIdHelp"
                       required>

                <p id="loginIdHelp" class="muted">
                    半角英数字、ハイフン、アンダースコアで、
                    4〜100文字で入力してください。
                </p>

                <label for="password">パスワード</label>
                <input type="password"
                       id="password"
                       name="password"
                       autocomplete="new-password"
                       minlength="8"
                       aria-describedby="passwordHelp"
                       required>

                <p id="passwordHelp" class="muted">
                    8文字以上で入力してください。
                    日本語などを含む場合も、UTF-8で72バイト以内にしてください。
                </p>

                <label for="passwordConfirm">
                    パスワード（確認）
                </label>
                <input type="password"
                       id="passwordConfirm"
                       name="passwordConfirm"
                       autocomplete="new-password"
                       minlength="8"
                       required>

                <button type="submit" class="primary">
                    登録する
                </button>
            </form>

            <c:url var="loginUrl"
                   value="/safety/auth/Login.action" />

            <a class="primary register-link"
               href="${loginUrl}">
                ログイン画面に戻る
            </a>
        </main>
    </div>
</body>
</html>