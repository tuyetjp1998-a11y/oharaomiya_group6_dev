<%-- エラーページ --%>
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

    <title>エラーページ | 学校安否確認</title>

    <link rel="stylesheet"
      href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <div class="phone">
        <header class="top">
            <div class="brand">学校安否確認</div>

            <c:if test="${not empty sessionScope.user}">
                <span>
                    <c:out value="${sessionScope.user.name}"/>様
                </span>
            </c:if>
        </header>

        <main class="page">
            <section class="card">
                <h1>エラーが発生しました</h1>

                <p>
                    処理を完了できませんでした。
                    しばらく時間をおいて、もう一度お試しください。
                </p>

                <c:choose>
                    <%-- ログイン済みの場合 --%>
                    <c:when test="${not empty sessionScope.user}">
                        <c:choose>
                            <c:when test="${sessionScope.user.role eq 'STUDENT'}">
                                <c:url var="homeUrl"
                                       value="/safety/student/Home.action"/>
                            </c:when>

                            <c:when test="${sessionScope.user.role eq 'FAMILY'}">
                                <c:url var="homeUrl"
                                       value="/safety/family/Home.action"/>
                            </c:when>

                            <c:when test="${sessionScope.user.role eq 'TEACHER'}">
                                <c:url var="homeUrl"
                                       value="/safety/teacher/Home.action"/>
                            </c:when>

                            <c:when test="${sessionScope.user.role eq 'PRINCIPAL'}">
                                <c:url var="homeUrl"
                                       value="/safety/principal/Home.action"/>
                            </c:when>

                            <c:otherwise>
                                <c:url var="homeUrl"
                                       value="/safety/auth/Login.action"/>
                            </c:otherwise>
                        </c:choose>

                        <a class="primary" href="${homeUrl}">
                            ホームへ戻る
                        </a>
                    </c:when>

                    <%-- 未ログインの場合 --%>
                    <c:otherwise>
                        <c:url var="loginUrl"
                               value="/safety/auth/Login.action"/>

                        <a class="primary" href="${loginUrl}">
                            ログイン画面へ
                        </a>
                    </c:otherwise>
                </c:choose>
            </section>

            <footer>
                <p class="muted">学校安否確認システム</p>
            </footer>
        </main>
    </div>
</body>
</html>