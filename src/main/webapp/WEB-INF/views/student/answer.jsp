<%-- 安否回答画面 --%>
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

    <title>安否回答 | 学校安否確認</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <c:url var="homeUrl"
           value="/safety/student/Home.action" />
    <c:url var="dangerUrl"
       		value="/safety/student/Danger.action" />

    <main class="page">
        <a class="back" href="${homeUrl}">
            ← 戻る
        </a>

        <h1>現在の状況</h1>

        <%-- 画面連携の確認段階のため、回答はまだ送信しない --%>
<c:url var="submitUrl"
       value="/safety/student/AnswerSubmit.action" />

<c:url var="dangerUrl"
       value="/safety/student/Danger.action" />

<div class="choices">
    <form method="post" action="${submitUrl}">
        <input type="hidden"
               name="csrfToken"
               value="${sessionScope.answerCsrfToken}">

        <input type="hidden"
               name="status"
               value="SAFE">

        <button type="submit" class="big safe">
            無事
        </button>
    </form>

    <a class="big danger" href="${dangerUrl}">
        危険
    </a>
</div>
    </main>
</body>
</html>