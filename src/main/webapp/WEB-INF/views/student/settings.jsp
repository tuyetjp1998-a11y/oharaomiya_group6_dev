<%-- 生徒の設定画面 --%>
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

    <title>設定 | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css?v=20261028-1">
</head>
<body class="a">
    <%-- 遷移先URL --%>
    <c:url var="homeUrl"
           value="/safety/student/Home.action" />
    <c:url var="moreUrl"
           value="/safety/student/More.action" />
    <c:url var="settingsSaveUrl"
           value="/safety/student/SettingsSave.action" />
    <c:url var="studentPushUrl"
           value="/safety/student/PushSubscription.action" />

    <div class="phone">
        <header class="phone-header">
            <div class="brand">AnpiLink</div>
        </header>

        <main class="page">
            <h1>設定</h1>

            <c:if test="${requestScope.settingsSaved}">
                <p role="status">設定を保存しました。</p>
            </c:if>

            <form action="${settingsSaveUrl}" method="post">
                <input type="hidden"
                       name="csrfToken"
                       value="<c:out value='${sessionScope.csrfToken}' />">

                <div class="card">
                    <%-- 位置情報の設定 --%>
                    <p id="student-location-warning"
                       class="student-setting-warning">
                        ※OFFにすると、安否回答に位置情報が付かなくなります。
                    </p>

                    <div class="row">
                        <label for="locationEnabled">
                            位置情報の利用
                        </label>

                        <select id="locationEnabled"
                                name="locationEnabled"
                                aria-describedby="student-location-warning student-location-help">
                            <c:choose>
                                <c:when test="${requestScope.locationEnabled}">
                                    <option value="true" selected>ON</option>
                                    <option value="false">OFF</option>
                                </c:when>
                                <c:otherwise>
                                    <option value="true">ON</option>
                                    <option value="false" selected>OFF</option>
                                </c:otherwise>
                            </c:choose>
                        </select>
                    </div>

                    <%-- ログイン期限通知の設定 --%>
                    <p id="student-notification-warning"
                       class="student-setting-warning">
                        ※ログイン期限のお知らせを受け取るため、
                        通知を許可してください。
                    </p>

                    <div class="row">
                        <span>ログイン期限の通知</span>

                        <button type="button"
                                id="student-notification-enable"
                                class="plain-button"
                                aria-describedby="student-notification-warning student-notification-message"
                                disabled>
                            通知を有効にする
                        </button>
                    </div>

                    <p id="student-notification-message"
                       class="muted"
                       role="status">
                        有効期限の7日前から、再ログインを通知します。
                    </p>
                </div>

                <p id="student-location-help" class="muted">
                    ONの場合、安否送信時に位置情報を取得します。
                    取得できない場合も安否は送信できます。
                </p>

                <button type="submit"
                        class="primary group-submit">
                    保存
                </button>
            </form>
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

    <script src="${pageContext.request.contextPath}/js/student-push.js"
            data-context-path="${pageContext.request.contextPath}"
            data-subscription-url="${studentPushUrl}"
            data-csrf-token="<c:out value='${sessionScope.csrfToken}' />"
            data-vapid-public-key="<c:out value='${requestScope.vapidPublicKey}' />"
            defer></script>

    <script>
        /* 位置情報をOFFにする前に確認する */
        (function () {
            "use strict";

            var select = document.getElementById(
                "locationEnabled"
            );

            if (!select) {
                return;
            }

            var previousValue = select.value;

            select.addEventListener("change", function () {
                if (select.value === "false") {
                    var confirmed = window.confirm(
                        "位置情報をOFFにすると、安否回答に位置情報が" +
                        "付かなくなります。OFFにしますか？"
                    );

                    if (!confirmed) {
                        select.value = previousValue;
                        return;
                    }
                }

                previousValue = select.value;
            });
        })();
    </script>
</body>
</html>