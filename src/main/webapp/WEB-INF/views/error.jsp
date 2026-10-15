<%-- 共通エラーページ --%>
<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"
         isErrorPage="true"
         session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%
    response.setHeader("Cache-Control", "no-store");
%>

<c:set var="errorCode"
       value="${requestScope['jakarta.servlet.error.status_code']}" />

<%-- エラーコードに対応する固定メッセージ --%>
<c:choose>
    <c:when test="${errorCode eq 400}">
        <c:set var="errorTitle"
               value="入力内容を確認してください" />
        <c:set var="errorMessage"
               value="リクエストが正しくありません。画面を開き直して、もう一度操作してください。" />
    </c:when>

    <c:when test="${errorCode eq 403}">
        <c:set var="errorTitle"
               value="アクセス権限がありません" />
        <c:set var="errorMessage"
               value="利用できるアカウントでログインしてください。" />
    </c:when>

    <c:when test="${errorCode eq 404}">
        <c:set var="errorTitle"
               value="ページが見つかりません" />
        <c:set var="errorMessage"
               value="URLが変更されたか、ページが削除された可能性があります。" />
    </c:when>

    <c:when test="${errorCode eq 405}">
        <c:set var="errorTitle"
               value="この操作は利用できません" />
        <c:set var="errorMessage"
               value="画面を開き直して、正しい手順で操作してください。" />
    </c:when>

    <c:otherwise>
        <c:set var="errorCode" value="500" />
        <c:set var="errorTitle"
               value="システムエラーが発生しました" />
        <c:set var="errorMessage"
               value="時間をおいて、もう一度お試しください。" />
    </c:otherwise>
</c:choose>

<%-- 新しいセッションを作成せず、既存の利用者を確認 --%>
<c:set var="currentUser"
       value="${pageContext.request.getSession(false).getAttribute('user')}" />

<c:url var="loginUrl"
       value="/safety/auth/Login.action" />

<c:set var="returnUrl" value="${loginUrl}" />
<c:set var="returnLabel" value="ログイン画面へ" />

<%-- 403以外は、自分の利用者区分のホームへ戻す --%>
<c:if test="${errorCode ne 403
              and not empty currentUser
              and currentUser.active}">

    <c:choose>
        <c:when test="${currentUser.role eq 'STUDENT'}">
            <c:url var="returnUrl"
                   value="/safety/student/Home.action" />
            <c:set var="returnLabel" value="ホームへ" />
        </c:when>

        <c:when test="${currentUser.role eq 'FAMILY'}">
            <c:url var="returnUrl"
                   value="/safety/family/Home.action" />
            <c:set var="returnLabel" value="ホームへ" />
        </c:when>

        <c:when test="${currentUser.role eq 'TEACHER'}">
            <c:url var="returnUrl"
                   value="/safety/teacher/Home.action" />
            <c:set var="returnLabel" value="ホームへ" />
        </c:when>

        <c:when test="${currentUser.role eq 'PRINCIPAL'}">
            <c:url var="returnUrl"
                   value="/safety/principal/Home.action" />
            <c:set var="returnLabel" value="ホームへ" />
        </c:when>
    </c:choose>
</c:if>

<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, initial-scale=1">

    <title><c:out value="${errorTitle}" /> | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/error.css?v=20261015-1">
</head>
<body>
    <header class="app-header">
        <div class="brand">AnpiLink</div>
    </header>

    <main class="error-main">
        <section class="error-card"
                 aria-labelledby="error-title">

            <p class="error-code">
                <c:out value="${errorCode}" />
            </p>

            <h1 id="error-title">
                <c:out value="${errorTitle}" />
            </h1>

            <p class="error-message"><c:out value="${errorMessage}" /></p>

            <a class="primary-button" href="${returnUrl}">
                <c:out value="${returnLabel}" />
            </a>
        </section>
    </main>

    <footer class="app-footer">
        ＠AnpiLink
    </footer>
</body>
</html>