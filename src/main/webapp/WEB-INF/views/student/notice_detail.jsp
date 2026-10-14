<%-- 学校からの連絡詳細画面 --%>
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

    <title>避難訓練のお知らせ | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="a">
    <%-- 遷移先URL --%>
    <c:url var="homeUrl"
           value="/safety/student/Home.action" />
    <c:url var="moreUrl"
           value="/safety/student/More.action" />
    <c:url var="noticesUrl"
           value="/safety/student/Notices.action" />

    <div class="phone">
        <header class="phone-header">
            <div class="brand">AnpiLink</div>
        </header>

        <main class="page">
            <h1>避難訓練のお知らせ</h1>

            <a class="back" href="${noticesUrl}">
                ← 学校からの連絡
            </a>

            <%-- DB連携前の画面確認用サンプル --%>
            <div class="card">
                10月9日に避難訓練を実施します。
            </div>
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