<%-- 生徒・保護者向けの通知設定案内画面 --%>
<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>通知の設定 | AnpiLink</title>

    <c:url var="styleUrl" value="/css/style.css" />
    <link rel="stylesheet" href="${styleUrl}">
</head>
<body class="a">
    <c:url var="introUrl" value="/safety/auth/NotificationIntro.action" />
    <c:url var="homeUrl" value="${requestScope.homePath}" />
    <c:url var="subscriptionUrl" value="${requestScope.subscriptionPath}" />
    <c:url var="pushScriptUrl" value="${requestScope.pushScriptPath}">
        <c:param name="v" value="notification-intro-1" />
    </c:url>

    <div class="phone">
        <header class="phone-header">
            <div class="brand">AnpiLink</div>
        </header>

        <main class="page notification-intro"
              id="${requestScope.setupId}">
            <p class="eyebrow">はじめに</p>
            <h1>通知を設定しましょう</h1>

            <div class="card">
                <p>ログインの有効期限が近づくと、期限の7日前から1日1回、再ログインを促す通知をお送りします。</p>
                <p>大切なお知らせを受け取るため、通知を許可してください。</p>
            </div>

            <button type="button"
                    id="${requestScope.enableId}"
                    class="primary notification-permission-button"
                    aria-describedby="${requestScope.messageId}"
                    disabled>
                通知を設定する
            </button>

            <p id="${requestScope.messageId}"
               class="notification-help"
               role="status"></p>

            <form action="${introUrl}" method="post">
                <input type="hidden"
                       name="csrfToken"
                       value="<c:out value='${sessionScope.csrfToken}' />">

                <button type="submit"
                        id="continue-without-notifications"
                        class="plain-button notification-later">
                    あとで設定する
                </button>
            </form>
        </main>

        <footer class="app-footer mobile-footer">＠AnpiLink</footer>
    </div>

    <script src="${pushScriptUrl}"
            data-context-path="${pageContext.request.contextPath}"
            data-subscription-url="${subscriptionUrl}"
            data-csrf-token="<c:out value='${sessionScope.csrfToken}' />"
            data-vapid-public-key="<c:out value='${requestScope.vapidPublicKey}' />"
            data-home-url="${homeUrl}"
            defer></script>
</body>
</html>