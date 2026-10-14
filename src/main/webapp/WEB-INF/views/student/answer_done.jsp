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

    <title>送信完了 | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="a">
    <%-- 遷移先URL --%>
    <c:url var="homeUrl"
           value="/safety/student/Home.action" />
    <c:url var="moreUrl"
           value="/safety/student/More.action" />
    
    <div class="phone">
        <header class="phone-header">
            <div class="brand">AnpiLink</div>
        </header>

        <main class="page">
            <h1>送信完了</h1>
            <h2>安否を送信しました</h2>

            <div class="card">
                <div class="row">
    <span>状況</span>

    <c:choose>
        <c:when test="${requestScope.report.status eq 'SAFE'}">
            <b class="status-badge safe">無事</b>
        </c:when>

        <c:when test="${requestScope.report.dangerType eq 'ACCIDENT'}">
            <b class="status-badge accident">事故</b>
        </c:when>

        <c:when test="${requestScope.report.dangerType eq 'EARTHQUAKE'}">
            <b class="status-badge earthquake">地震</b>
        </c:when>

        <c:when test="${requestScope.report.dangerType eq 'FIRE'}">
            <b class="status-badge fire">火災</b>
        </c:when>

        <c:when test="${requestScope.report.dangerType eq 'LANDSLIDE'}">
            <b class="status-badge landslide">土砂崩れ</b>
        </c:when>

        <c:when test="${requestScope.report.dangerType eq 'STALKING'}">
            <b class="status-badge stalker">
                付きまとわれている
            </b>
        </c:when>
    </c:choose>
</div>

                <div class="row">
                    <span>送信日時</span>

                    <span>
                        <c:out value="${requestScope.reportedAtText}" />
                    </span>
                </div>

                <div class="row">
    <span>位置情報</span>

    <c:choose>
        <c:when test="${requestScope.report.latitude ne null
                       and requestScope.report.longitude ne null}">
            <span>
                緯度：
                <c:out value="${requestScope.report.latitude}" />
                <br>
                経度：
                <c:out value="${requestScope.report.longitude}" />
            </span>
        </c:when>

        <c:otherwise>
            <span class="muted">未取得</span>
        </c:otherwise>
    </c:choose>
</div>
            </div>

            <a class="primary group-submit" href="${homeUrl}">
                ホームへ
            </a>
        </main>

        <nav class="phone-tabs tabs two"
             aria-label="メインメニュー">
            <a href="${homeUrl}">
                <svg viewBox="0 0 24 24"
                     aria-hidden="true"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="1.7"
                     stroke-linecap="round"
                     stroke-linejoin="round">
                    <path d="m3 10 9-7 9 7M5 9v12h14V9M9 21v-7h6v7" />
                </svg>

                <span>ホーム</span>
            </a>

            <a href="${moreUrl}">
                <svg viewBox="0 0 24 24"
                     aria-hidden="true"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="1.7"
                     stroke-linecap="round"
                     stroke-linejoin="round">
                    <path d="M5 6h14M5 12h14M5 18h14" />
                </svg>

                <span>その他</span>
            </a>
        </nav>
    </div>

    
   
</body>
</html>