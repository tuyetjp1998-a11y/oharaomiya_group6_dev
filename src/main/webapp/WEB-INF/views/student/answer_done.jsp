<%-- 安否回答完了画面 --%>
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

    <title>回答完了 | 学校安否確認</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <c:url var="homeUrl"
           value="/safety/student/Home.action" />

    <main class="page">
        <h1>
            <c:out value="${statusLabel}" />を送信しました
        </h1>

        <div class="card">
            <div class="row">
                <span>状況</span>
                <b><c:out value="${statusLabel}" /></b>
            </div>

            <c:if test="${report.status == 'DANGER'}">
                <div class="row">
                    <span>危険の種類</span>
                    <b style="color: #c94747">
                        <c:out value="${dangerLabel}" />
                    </b>
                </div>
            </c:if>

            <div class="row">
                <span>送信時刻</span>
                <span>
                    <c:out value="${reportedAtText}" />
                </span>
            </div>

            <div class="row">
                <span>位置情報</span>
                <span>未取得</span>
            </div>
        </div>

        <a class="primary"
           style="margin-top: 16px"
           href="${homeUrl}">
            ホームへ
        </a>
    </main>
</body>
</html>