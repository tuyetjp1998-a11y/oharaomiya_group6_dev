<%-- ログイン動作確認用のトップ画面 --%>
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

    <title>
        <c:out value="${requestScope.pageTitle}" />
        | 学校安否確認
    </title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <header class="top">
        <div class="brand">学校安否確認</div>

        <div class="header-user">
            <span>
                <c:out value="${sessionScope.user.name}" />様
            </span>

            <c:url var="logoutUrl"
                   value="/safety/auth/Logout.action" />

            <form action="${logoutUrl}"
                  method="post"
                  onsubmit="return confirm('ログアウトしますか？');">

                <input type="hidden"
                       name="csrfToken"
                       value="${sessionScope.csrfToken}">

                <button type="submit" class="logout">
                    ログアウト
                </button>
            </form>
        </div>
    </header>

    <main class="page">
        <h1>
            <c:out value="${requestScope.pageTitle}" />
        </h1>

        <section class="card">
            <p>ログインに成功しました。</p>

            <p>
                氏名：
                <c:out value="${sessionScope.user.name}" />
            </p>

            <p>
                ログインID：
                <c:out value="${sessionScope.user.loginId}" />
            </p>

            <p>
                利用者区分：
                <c:out value="${sessionScope.user.role}" />
            </p>
        </section>
    </main>
</body>
</html>