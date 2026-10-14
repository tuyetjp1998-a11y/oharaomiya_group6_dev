<%-- 生徒の通信状態デモ画面 --%>
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
        ${requestScope.onlineDemo ? '通信復旧' : '通信状態'}
        | AnpiLink
    </title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css?v=20261014-1">
</head>
<body class="a">
    <%-- 遷移先URL --%>
    <c:url var="homeUrl"
           value="/safety/student/Home.action" />
    <c:url var="moreUrl"
           value="/safety/student/More.action" />

    <c:url var="onlineDemoUrl"
           value="/safety/student/Connection.action">
        <c:param name="view" value="online" />
    </c:url>

    <div class="phone">
        <header class="phone-header">
            <div class="brand">AnpiLink</div>
        </header>

        <main class="page">
            <c:choose>
                <c:when test="${requestScope.onlineDemo}">
                    <h1>通信復旧</h1>

                    <div class="card">
                        <b>通信復旧（デモ）</b>
                        <p>通信復旧後の表示例です。</p>
                        <p class="muted">
                            実際の回答送信は行っていません。
                        </p>
                    </div>
                </c:when>

                <c:otherwise>
                    <h1>通信状態</h1>

                    <div class="card">
                        <b>オフライン（デモ）</b>
                        <p>一時保存の表示例</p>
                        <p class="muted">
                            実際の通信状態を示すものではありません。
                            一時保存機能は準備中です。
                        </p>
                    </div>

                    <a class="primary group-submit"
                       href="${onlineDemoUrl}">
                        通信復旧デモ
                    </a>
                </c:otherwise>
            </c:choose>
        </main>

        <footer class="app-footer mobile-footer">
            ＠AnpiLink
        </footer>

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