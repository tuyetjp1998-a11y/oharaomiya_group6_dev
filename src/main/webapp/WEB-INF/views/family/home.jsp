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
          href="${pageContext.request.contextPath}/css/style.css?v=20261015-1">
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

            <div class="card">
                <p class="muted">
                    お子さんの安否を表示する機能は準備中です。
                </p>
            </div>

            <button type="button"
                    class="primary group-submit"
                    disabled>
                お子さんのアカウントを代理登録
            </button>

            <p class="muted">
                代理登録機能は準備中です。
            </p>
        </main>

        <footer class="app-footer mobile-footer">
            ＠AnpiLink
        </footer>

        <nav class="phone-tabs tabs"
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

            <a role="link" aria-disabled="true">
                <svg viewBox="0 0 24 24"
                     aria-hidden="true"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="1.7"
                     stroke-linecap="round"
                     stroke-linejoin="round">
                    <circle cx="9" cy="8" r="3" />
                    <path d="M3 20v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 5" />
                </svg>
                <span>お子さんの回答</span>
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