<%-- 保護者の設定画面 --%>
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

    <title>お子さんの追加申請 | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="a">
    <c:url var="homeUrl"
           value="/safety/family/Home.action" />
    <c:url var="moreUrl"
           value="/safety/family/More.action" />
	<c:url var="childRequestUrl"
       value="/safety/family/ChildRequest.action" />
	<c:url var="settingsUrl"
       value="/safety/family/Settings.action" />
	<c:url var="childRequestUrl"
       value="/safety/family/ChildRequest.action" />
	
    <div class="phone">
        <header class="phone-header">
            <div class="brand">AnpiLink</div>
        </header>

     <main class="page">
    <a class="back" href="${settingsUrl}">← 設定</a>

    <h1>お子さんの追加申請</h1>

    <form id="parent-child-request"
          class="card form request-form"
          action="${childRequestUrl}"
          method="post">

        <input type="hidden"
               name="csrfToken"
               value="${sessionScope.csrfToken}">

        <label for="child-id">お子さんの生徒ID</label>

        <input type="text"
               id="child-id"
               name="studentLoginId"
               maxlength="40"
               autocomplete="off"
               required>

        <c:if test="${not empty requestScope.childRequestMessage}">
            <p id="parent-request-error"
               class="form-error"
               role="alert">
                <c:out value="${requestScope.childRequestMessage}" />
            </p>
        </c:if>

        <button class="primary group-submit" type="submit">
            申請する
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