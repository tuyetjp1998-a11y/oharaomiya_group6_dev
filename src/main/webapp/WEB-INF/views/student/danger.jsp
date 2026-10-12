<%-- 危険の詳細画面 --%>
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

    <title>危険の詳細 | 学校安否確認</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <c:url var="answerUrl"
           value="/safety/student/Answer.action" />

    <main class="page">
        <a class="back" href="${answerUrl}">← 戻る</a>

        <h1>危険の詳細</h1>

        <c:url var="submitUrl"
       value="/safety/student/AnswerSubmit.action" />

<form method="post" action="${submitUrl}">
    <input type="hidden"
           name="csrfToken"
           value="${sessionScope.answerCsrfToken}">

    <input type="hidden"
           name="status"
           value="DANGER">

    <div class="detail">
        <button type="submit"
                name="dangerType"
                value="ACCIDENT"
                class="accident">
            事故
        </button>

        <button type="submit"
                name="dangerType"
                value="EARTHQUAKE"
                class="earthquake">
            地震
        </button>

        <button type="submit"
                name="dangerType"
                value="FIRE"
                class="fire">
            火災
        </button>

        <button type="submit"
                name="dangerType"
                value="LANDSLIDE"
                class="landslide">
            土砂崩れ
        </button>

        <button type="submit"
                name="dangerType"
                value="STALKING"
                class="stalker full">
            付きまとわれている
        </button>
    </div>
</form>

        <p class="muted">回答の送信機能は準備中です。</p>
    </main>
</body>
</html>