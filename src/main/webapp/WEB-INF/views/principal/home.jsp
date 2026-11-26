<%@ page language="java"
    contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>管理ホーム | AnpiLink</title>

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

        <a class="nav-item active" href="${homeUrl}" aria-current="page">
            <svg viewBox="0 0 24 24" aria-hidden="true"
                 fill="none" stroke="currentColor" stroke-width="1.7"
                 stroke-linecap="round" stroke-linejoin="round">
                <path d="m3 10 9-7 9 7M5 9v12h14V9M9 21v-7h6v7"/>
            </svg>
            <span>ホーム</span>
        </a>

        <a class="nav-item" href="${answersUrl}">
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

        <div class="portal-heading">
            <div>
                <p class="eyebrow">管理ホーム</p>
                <h1>全体の安否状況</h1>
            </div>
            <div class="portal-date">
                <c:out value="${homeData.displayDate}" />
                <span>最終回答 <c:out value="${homeData.latestAnsweredAt}" /></span>
            </div>
        </div>

        <section class="stats" aria-label="全校の安否集計">
            <div class="stat">
                <span>回答済み</span>
                <div>
                    <strong><c:out value="${homeData.answeredCount}" /></strong><small> / <c:out value="${homeData.studentCount}" />人</small>
                </div>
                <div class="progress" role="progressbar" aria-label="回答率"
                     aria-valuemin="0" aria-valuemax="100" aria-valuenow="${homeData.progressPercent}">
                    <i style="width:${homeData.progressPercent}%"></i>
                </div>
            </div>
            <div class="stat">
                <span>未回答</span>
                <div><strong><c:out value="${homeData.unansweredCount}" /></strong><small>人</small></div>
                <p><c:choose>
                    <c:when test="${homeData.studentCount == 0}">対象の生徒はいません</c:when>
                    <c:when test="${homeData.unansweredCount == 0}">全員回答済みです</c:when>
                    <c:otherwise>回答が必要です</c:otherwise>
                </c:choose></p>
            </div>
            <div class="stat danger-stat">
                <span>要対応</span>
                <div><strong><c:out value="${homeData.attentionCount}" /></strong><small>人</small></div>
                <p>
                    <svg viewBox="0 0 24 24" aria-hidden="true" fill="none"
                         stroke="currentColor" stroke-width="1.7"
                         stroke-linecap="round" stroke-linejoin="round">
                        <path d="m12 3 10 18H2ZM12 9v5m0 3v.1"/>
                    </svg>
                    <c:choose>
                        <c:when test="${homeData.attentionCount > 0}">確認が必要です</c:when>
                        <c:otherwise>要対応の生徒はいません</c:otherwise>
                    </c:choose>
                </p>
            </div>
        </section>

        <div class="portal-content">
            <section class="alarm-panel">
                <div class="section-heading">
                    <h2>
                        <svg viewBox="0 0 24 24" aria-hidden="true" fill="none"
                         stroke="currentColor" stroke-width="1.7"
                         stroke-linecap="round" stroke-linejoin="round">
                        <path d="m12 3 10 18H2ZM12 9v5m0 3v.1"/>
                    </svg>
                        危険・困っている生徒
                    </h2>
                    <span class="count"><c:out value="${homeData.attentionCount}" />人</span>
                </div>
                <div class="danger-list">
                    <c:choose>
                        <c:when test="${empty homeData.attentionStudents}">
                            <p class="muted">危険・困っている生徒はいません。</p>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="student" items="${homeData.attentionStudents}">
                                <c:url var="studentDetailUrl" value="/safety/principal/AnswerDetail.action">
                                    <c:param name="studentId" value="${student.studentUserId}" />
                                    <c:param name="from" value="home" />
                                </c:url>
                                <div class="danger-person">
                                    <span class="avatar" aria-hidden="true"><c:out value="${student.avatar}" /></span>
                                    <div>
                                        <strong><c:out value="${student.name}" /></strong><span class="person-class"><c:out value="${student.className}" /></span>
                                    </div>
                                    <span class="badge ${student.statusClass}"><c:out value="${student.statusLabel}" /></span>
                                    <a class="detail-button" href="<c:out value='${studentDetailUrl}' />">
                                        確認
                                        <svg viewBox="0 0 24 24" aria-hidden="true" fill="none"
                     stroke="currentColor" stroke-width="1.7"
                     stroke-linecap="round" stroke-linejoin="round">
                    <path d="M5 12h14m-5-5 5 5-5 5"/>
                </svg>
                                    </a>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </section>
            <section class="answers-panel">
                <div class="section-heading">
                    <h2>最新の安否回答</h2>
                    <a class="text-button" href="${answersUrl}">
                        一覧を見る
                        <svg viewBox="0 0 24 24" aria-hidden="true" fill="none"
                     stroke="currentColor" stroke-width="1.7"
                     stroke-linecap="round" stroke-linejoin="round">
                    <path d="M5 12h14m-5-5 5 5-5 5"/>
                </svg>
                    </a>
                </div>
                <div class="table-scroll">
                    <table>
                        <thead>
                            <tr>
                                <th scope="col">生徒</th>
                                <th scope="col">クラス</th>
                                <th scope="col">安否</th>
                                <th scope="col">回答時刻</th>
                                <th scope="col"></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty homeData.latestAnswers}">
                                    <tr><td colspan="5">安否回答はまだありません。</td></tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="student" items="${homeData.latestAnswers}">
                                        <c:url var="studentDetailUrl" value="/safety/principal/AnswerDetail.action">
                                            <c:param name="studentId" value="${student.studentUserId}" />
                                            <c:param name="from" value="home" />
                                        </c:url>
                                        <tr>
                                            <td><span class="table-name"><c:out value="${student.name}" /></span></td>
                                            <td><c:out value="${student.className}" /></td>
                                            <td><span class="badge ${student.statusClass}"><c:out value="${student.statusLabel}" /></span></td>
                                            <td><c:out value="${student.reportedAtLabel}" /></td>
                                            <td>
                                                <a class="table-more" href="<c:out value='${studentDetailUrl}' />">
                                                    詳細
                                                    <svg viewBox="0 0 24 24" aria-hidden="true" fill="none"
                     stroke="currentColor" stroke-width="1.7"
                     stroke-linecap="round" stroke-linejoin="round">
                    <path d="M5 12h14m-5-5 5 5-5 5"/>
                </svg>
                                                </a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </section>
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
