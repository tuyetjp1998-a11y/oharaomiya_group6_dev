<%-- 保護者ホーム画面 --%>
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

    <title>保護者ホーム | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="a">
    <c:url var="homeUrl"
           value="/safety/family/Home.action" />
    <c:url var="moreUrl"
           value="/safety/family/More.action" />

    <div class="phone">
        <header class="phone-header">
            <div class="brand">AnpiLink</div>
        </header>

        <main class="page">
            <h1>保護者ホーム</h1>

            <div class="who">
                保護者　
                <c:out value="${sessionScope.user.name}" />さん
            </div>

            <h2>お子さんの安否</h2>

<c:choose>
    <c:when test="${empty requestScope.students}">
        <p class="muted">
            関連付けられている生徒はいません。
        </p>
    </c:when>

    <c:otherwise>
        <c:forEach var="student"
                   items="${requestScope.students}">

            <%-- 詳細画面は次の段階で連携する --%>
            <c:url var="childDetailUrl"
       value="/safety/family/ChildDetail.action">
    <c:param name="studentId"
             value="${student.studentUserId}" />
</c:url>

<a class="card child-card" href="${childDetailUrl}">
                <b>
                    <c:out value="${student.studentName}" />
                </b>

                <c:choose>
                    <c:when test="${student.status eq 'SAFE'}">
                        <span class="status-badge safe">
                            無事
                        </span>
                    </c:when>

                    <c:when test="${student.status eq 'DANGER'}">
                        <span class="status-badge danger">
                            危険
                        </span>
                    </c:when>

                    <c:otherwise>
                        <span class="status-badge">
                            未回答
                        </span>
                    </c:otherwise>
                </c:choose>
                <span aria-hidden="true">›</span>
</a>
        </c:forEach>
    </c:otherwise>
</c:choose>
        </main>

        <footer class="app-footer mobile-footer">
            ＠AnpiLink
        </footer>

        <nav class="phone-tabs tabs two"
             aria-label="メインメニュー">
            <a class="active"
               href="${homeUrl}"
               aria-current="page">
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