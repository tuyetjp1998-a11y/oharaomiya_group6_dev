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

    <title>現在の状況 | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="a">
    <%-- 遷移先URL --%>
    <c:url var="homeUrl"
           value="/safety/student/Home.action" />
    <c:url var="moreUrl"
           value="/safety/student/More.action" />
    
    <c:url var="submitUrl"
      	   value="/safety/student/AnswerSubmit.action" />
	<c:url var="dangerUrl"
       	    value="/safety/student/Danger.action" />

    <div class="phone">
        <header class="phone-header">
            <div class="brand">AnpiLink</div>
        </header>

        <main class="page">
            <h1>現在の状況</h1>

            <a class="back" href="${homeUrl}">
                ← ホーム
            </a>

            <%-- 送信処理との連携が完了するまで無効にする --%>
            <div class="choices">
    <form action="${submitUrl}" method="post"
    		class="answer-location-form">
        <input type="hidden"
               name="csrfToken"
               value="${sessionScope.answerCsrfToken}">

        <input type="hidden"
               name="status"
               value="SAFE">
         <input type="hidden" name="latitude" value="">
<input type="hidden" name="longitude" value="">
<input type="hidden" name="locationAccuracy" value="">
<input type="hidden" name="locationAcquiredAt" value="">

<p class="muted location-message"
   role="status"
   aria-live="polite"></p>

        <button type="submit"
                class="big safe choice-button">
            無事
        </button>
    </form>

    <a class="big danger choice-button"
       href="${dangerUrl}">
        危険
    </a>
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

    <%-- ログアウト確認ダイアログ --%>
    <div class="modal"
         id="logout-dialog"
         role="dialog"
         aria-modal="true"
         aria-labelledby="logout-title"
         aria-hidden="true">
        <div class="dialog">
            <h2 id="logout-title">ログアウトしますか？</h2>

            <form action="${logoutUrl}" method="post">
                <input type="hidden"
                       name="csrfToken"
                       value="${sessionScope.csrfToken}">

                <div>
                    <button type="button"
                            class="plain-button"
                            id="logoutCancel"
                            onclick="closeLogoutDialog()">
                        キャンセル
                    </button>

                    <button type="submit"
                            class="delete-button">
                        ログアウト
                    </button>
                </div>
            </form>
        </div>
    </div>
    
   <script src="${pageContext.request.contextPath}/js/answer-location.js?v=20261014-1"></script> 
</body>
</html>