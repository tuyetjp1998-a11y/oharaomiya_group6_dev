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

    <title>設定 | AnpiLink</title>

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
	
    <div class="phone">
        <header class="phone-header">
            <div class="brand">AnpiLink</div>
        </header>

        <main class="page">
            <h1>設定</h1>
            <c:if test="${not empty requestScope.childRequestMessage}">
    <p role="status">
        <c:out value="${requestScope.childRequestMessage}" />
    </p>
</c:if>

            <div class="card">
                <div class="row">
                    <span>通知</span>
                    <span class="muted">準備中</span>
                </div>
            </div>

            <h2>お子さん</h2>

            <c:choose>
                <c:when test="${empty requestScope.students}">
                    <div class="card">
                        <span class="muted">
                            関連付けられている生徒はいません。
                        </span>
                    </div>
                </c:when>

                <c:otherwise>
                    <c:forEach var="student"
                               items="${requestScope.students}">
                        <div class="card">
                            <c:out value="${student.studentName}" />
                            （<c:out value="${student.studentLoginId}" />）
                        </div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>

            <%-- 生徒の追加処理は次の段階で実装する --%>
            <%-- 承認待ちの申請 --%>
<c:if test="${not empty requestScope.pendingRequests}">
    <h2>承認待ち</h2>
    
    <%-- 審査済みの申請履歴 --%>
<h2>申請履歴</h2>

<c:choose>
    <c:when test="${empty requestScope.reviewedRequests}">
        <div class="card">
            <p class="muted">
                審査済みの申請はありません。
            </p>
        </div>
    </c:when>

    <c:otherwise>
        <p class="muted">最新20件を表示しています。</p>

        <c:forEach var="history"
                   items="${requestScope.reviewedRequests}">
            <section class="card">
                <div class="row">
                    <span>生徒ID</span>
                    <b>
                        <c:out value="${history.studentLoginId}" />
                    </b>
                </div>

                <div class="row">
                    <span>審査結果</span>

                    <c:choose>
                        <c:when test="${history.status eq 'APPROVED'}">
                            <span class="status-badge safe">
                                承認済み
                            </span>
                        </c:when>

                        <c:when test="${history.status eq 'REJECTED'}">
                            <span class="status-badge danger">
                                却下
                            </span>
                        </c:when>
                    </c:choose>
                </div>

                <div class="row">
                    <span>申請日時</span>
                    <span>
                        <c:out value="${history.requestedAtText}" />
                    </span>
                </div>

                <div class="row">
                    <span>審査日時</span>
                    <span>
                        <c:out value="${history.reviewedAtText}" />
                    </span>
                </div>

                <c:if test="${history.status eq 'REJECTED'}">
                    <p class="muted">
                        申請については学校へお問い合わせください。
                    </p>
                </c:if>
            </section>
        </c:forEach>
    </c:otherwise>
</c:choose>

    <c:forEach var="pending"
               items="${requestScope.pendingRequests}">
        <div class="card">
            <div class="row">
                <span>
                    <c:out value="${pending.studentLoginId}" />
                </span>
                <span class="status-badge">承認待ち</span>
            </div>
        </div>
    </c:forEach>
</c:if>

<form class="card form"
      action="${childRequestUrl}"
      method="post">

    <input type="hidden"
           name="csrfToken"
           value="${sessionScope.csrfToken}">

    <label for="child-id">お子さんの生徒ID</label>

    <input type="text"
           id="child-id"
           name="studentLoginId"
           maxlength="100"
           required
           aria-describedby="child-request-help">

    <button type="submit"
            class="primary group-submit">
        追加する
    </button>

    <p class="muted" id="child-request-help">
        教員の承認後に、お子さんの安否情報を確認できます。
    </p>
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