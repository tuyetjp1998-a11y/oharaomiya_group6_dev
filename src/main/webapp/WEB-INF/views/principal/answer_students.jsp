<%@ page language="java"
    contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title><c:out value="${classData.className}" />の安否確認 | AnpiLink</title>

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
            <span>通知配信</span>
        </a>

        <a class="nav-item" href="${pageContext.request.contextPath}/safety/principal/Groups.action">
            <svg viewBox="0 0 24 24" aria-hidden="true"
                 fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
                <circle cx="9" cy="8" r="3"/>
                <path d="M3 20v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 5"/>
            </svg>
            <span>グループ管理</span>
        </a>

        <a class="nav-item" href="${pageContext.request.contextPath}/safety/principal/Staff.action">
            <svg viewBox="0 0 24 24" aria-hidden="true"
                 fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
                <circle cx="9" cy="8" r="3"/>
                <path d="M3 20v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 5"/>
            </svg>
            <span>職員管理</span>
        </a>

        <a class="nav-item" href="${pageContext.request.contextPath}/safety/principal/DangerTypes.action">
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

        <nav class="safety-breadcrumb" aria-label="現在の位置">
            <a href="${answersUrl}">全体の安否確認</a>
            <span aria-hidden="true">›</span>

            <a href="${classesUrl}?grade=${classData.grade}">
                <c:out value="${classData.grade}" />年生
            </a>

            <span aria-hidden="true">›</span>
            <span><c:out value="${classData.className}" /></span>
        </nav>

        <h1><c:out value="${classData.className}" />の安否確認</h1>

        <a class="back"
           href="${classesUrl}?grade=${classData.grade}">
            ← <c:out value="${classData.grade}" />年生のクラス一覧
        </a>

        <p class="safety-summary">
            生徒 <c:out value="${classData.studentCount}" />人　
            回答済み <c:out value="${classData.answeredCount}" />人　
            未回答 <c:out value="${classData.unansweredCount}" />人　
            要対応 <c:out value="${classData.attentionCount}" />人
        </p>

        <h2>生徒</h2>

        <div class="table">
            <c:choose>
                <c:when test="${empty classData.students}">
                    <p class="muted">
                        このクラスには生徒が登録されていません。
                    </p>
                </c:when>

                <c:otherwise>
                    <c:forEach var="student" items="${classData.students}">
                        <a class="row"
                           href="${detailUrl}?studentId=${student.studentUserId}&amp;classId=${classData.classId}&amp;schoolYear=${classData.schoolYear}">

                            <span><c:out value="${student.name}" /></span>
                            <span><c:out value="${student.loginId}" /></span>

                            <b class="status-badge ${student.statusClass}">
                                <c:out value="${student.statusLabel}" />
                            </b>

                            <span aria-hidden="true">›</span>
                        </a>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
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
