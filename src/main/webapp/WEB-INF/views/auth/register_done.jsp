<%-- 保護者の登録完了画面 --%>
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

    <title>登録完了 | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="a">
    <c:url var="loginUrl"
           value="/safety/auth/Login.action" />

    <main class="static-chooser">
        <h1 class="login-brand">AnpiLink</h1>
        <h2>登録完了</h2>

        <p>保護者アカウントを登録しました。</p>

        <span class="status-badge request-pending">
            親子関係の承認待ち
        </span>

        <p>職員の承認後に閲覧できます。</p>

        <a class="primary group-submit" href="${loginUrl}">
            ログインへ
        </a>
    </main>
</body>
</html>