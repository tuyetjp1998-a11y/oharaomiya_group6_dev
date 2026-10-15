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
          href="${pageContext.request.contextPath}/css/style.css?v=20261014-1">
</head>
<body class="a">
    <%-- 遷移先URL --%>
    <c:url var="homeUrl"
           value="/safety/student/Home.action" />
    <c:url var="moreUrl"
           value="/safety/student/More.action" />
	<c:url var="settingsSaveUrl"
       value="/safety/student/SettingsSave.action" />
       
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
               value="${sessionScope.csrfToken}">

        <div class="card">
            <div class="row">
                <label for="locationEnabled">
                    位置情報の利用
                </label>

                <select id="locationEnabled"
                        name="locationEnabled">
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

            <div class="row">
                <span>通知</span>
                <span class="muted">準備中</span>
            </div>
        </div>

        <p class="muted">
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
</body>
</html>