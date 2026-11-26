<%@ page language="java"
    contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>回答詳細 | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css?v=20261124-1">
</head>

<body class="a admin-portal">

<c:url var="homeUrl" value="/safety/principal/Home.action" />
<c:url var="answersUrl" value="/safety/principal/Answers.action" />
<c:url var="classesUrl" value="/safety/principal/Classes.action" />
<c:url var="detailUrl" value="/safety/principal/AnswerDetail.action" />
<c:url var="logoutUrl" value="/safety/auth/Logout.action" />

<header class="portal-header">
    <div class="brand">AnpiLink</div>

    <div class="portal-user">
        <span class="user-dot" aria-hidden="true">校</span>
        <span><c:out value="${sessionScope.user.name}" />校長</span>

        <button type="button"
                class="logout"
                onclick="openLogoutDialog()">
            ログアウト
        </button>
    </div>
</header>

<div class="portal-layout">

    <aside class="portal-nav">
        <span class="nav-caption">管理ポータル</span>

        <a class="nav-item" href="${homeUrl}">
            <svg viewBox="0 0 24 24" aria-hidden="true"
                 fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
                <path d="m3 10 9-7 9 7M5 9v12h14V9M9 21v-7h6v7"/>
            </svg>
            <span>ホーム</span>
        </a>

        <a class="nav-item active"
           href="${answersUrl}"
           aria-current="page">
            <svg viewBox="0 0 24 24" aria-hidden="true"
                 fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
                <path d="m5 12 4 4L19 6"/>
            </svg>
            <span>全体の安否確認</span>
        </a>

        <a class="nav-item" href="${pageContext.request.contextPath}/safety/principal/Distribution.action">
            <svg viewBox="0 0 24 24" aria-hidden="true"
                 fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
                <path d="m12 3 10 18H2ZM12 9v5m0 3v.1"/>
            </svg>
            <span>安否確認</span>
        </a>

        <a class="nav-item" href="#" aria-disabled="true">
            <svg viewBox="0 0 24 24" aria-hidden="true"
                 fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
                <circle cx="9" cy="8" r="3"/>
                <path d="M3 20v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 5"/>
            </svg>
            <span>グループ管理</span>
        </a>

        <a class="nav-item" href="#" aria-disabled="true">
            <svg viewBox="0 0 24 24" aria-hidden="true"
                 fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
                <circle cx="9" cy="8" r="3"/>
                <path d="M3 20v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 5"/>
            </svg>
            <span>職員管理</span>
        </a>

        <a class="nav-item" href="#" aria-disabled="true">
            <svg viewBox="0 0 24 24" aria-hidden="true"
                 fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
                <path d="m12 3 10 18H2ZM12 9v5m0 3v.1"/>
            </svg>
            <span>困っている内容</span>
        </a>

        <footer class="app-footer portal-footer">＠AnpiLink</footer>
    </aside>

    <main class="portal-main">

        <c:if test="${not empty student.classId}">
            <c:url var="studentListUrl" value="/safety/principal/Students.action">
                <c:param name="classId" value="${student.classId}" />
                <c:param name="schoolYear" value="${student.schoolYear}" />
            </c:url>
        </c:if>
        <nav class="safety-breadcrumb" aria-label="現在の位置">
            <c:choose>
                <c:when test="${empty student.classId}">
                    <a href="${homeUrl}">管理ホーム</a>
                </c:when>
                <c:otherwise>
                    <a href="${answersUrl}">全体の安否確認</a>
                    <c:if test="${student.grade > 0}">
                        <span aria-hidden="true">›</span>
                        <a href="${classesUrl}?grade=${student.grade}"><c:out value="${student.grade}" />年生</a>
                    </c:if>
                    <span aria-hidden="true">›</span>
                    <a href="<c:out value='${studentListUrl}' />"><c:out value="${student.className}" /></a>
                </c:otherwise>
            </c:choose>
            <span aria-hidden="true">›</span>
            <span aria-current="page"><c:out value="${student.name}" /></span>
        </nav>
        <h1>回答詳細</h1>
        <c:choose>
            <c:when test="${empty student.classId}">
                <a class="back" href="${homeUrl}">← 管理ホームに戻る</a>
            </c:when>
            <c:otherwise>
                <a class="back" href="<c:out value='${studentListUrl}' />">← <c:out value="${student.className}" />の生徒一覧</a>
            </c:otherwise>
        </c:choose>
        <div class="card">
            <div class="row"><span>氏名</span><b><c:out value="${student.name}" /></b></div>
            <div class="row"><span>生徒ID</span><span><c:out value="${student.loginId}" /></span></div>
            <div class="row"><span>クラス</span><span><c:out value="${student.className}" /></span></div>
            <div class="row"><span>安否</span><b class="status-badge ${student.statusClass}"><c:out value="${student.statusLabel}" /></b></div>
            <c:if test="${student.status == 'WORRIED' and not empty student.comment}">
                <div class="row"><span>困っている内容</span><span style="white-space:pre-wrap; overflow-wrap:anywhere"><c:out value="${student.comment}" /></span></div>
            </c:if>
            <div class="row"><span>送信日時</span><span><c:out value="${student.detailTimeLabel}" /></span></div>
            <div class="row"><span>送信位置</span><span>
                <c:choose>
                    <c:when test="${empty student.reportId}">—</c:when>
                    <c:otherwise><c:out value="${student.locationLabel}" /></c:otherwise>
                </c:choose>
            </span></div>
        </div>
    </main>
</div>

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

<script>
const logoutModal = document.getElementById("logout-dialog");

function openLogoutDialog() {
    logoutModal.classList.add("show");
    logoutModal.setAttribute("aria-hidden", "false");
}

function closeLogoutDialog() {
    logoutModal.classList.remove("show");
    logoutModal.setAttribute("aria-hidden", "true");
}

document.addEventListener("keydown", function(event) {
    if (event.key === "Escape") {
        closeLogoutDialog();
    }
});
</script>

</body>
</html>
