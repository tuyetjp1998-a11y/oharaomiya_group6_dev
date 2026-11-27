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

    <title>
        <c:out value="${grade}" />年生の安否確認 | AnpiLink
    </title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css?v=20261124-2">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/principal-answers.css?v=20261124-1">
</head>

<body class="a admin-portal">

<c:url var="homeUrl"
       value="/safety/principal/Home.action" />

<c:url var="answersUrl"
       value="/safety/principal/Answers.action" />

<c:url var="classesUrl"
       value="/safety/principal/Classes.action" />

<c:url var="studentsUrl"
       value="/safety/principal/Students.action" />

<c:url var="logoutUrl"
       value="/safety/auth/Logout.action" />

<header class="portal-header">

    <div class="brand">
        AnpiLink
    </div>

    <div class="portal-user">

        <span class="user-dot"
              aria-hidden="true">
            校
        </span>

        <span>
            <c:out value="${sessionScope.user.name}" />校長
        </span>

        <button type="button"
                class="logout"
                onclick="openLogoutDialog()">
            ログアウト
        </button>

    </div>

</header>

<div class="portal-layout">

    <aside class="portal-nav">

        <span class="nav-caption">
            管理ポータル
        </span>

        <a class="nav-item"
           href="${homeUrl}">

            <svg viewBox="0 0 24 24"
                 aria-hidden="true"
                 fill="none"
                 stroke="currentColor"
                 stroke-width="1.7"
                 stroke-linecap="round"
                 stroke-linejoin="round">

                <path d="m3 10 9-7 9 7M5 9v12h14V9M9 21v-7h6v7"/>

            </svg>

            <span>ホーム</span>
        </a>

        <a class="nav-item active"
           href="${answersUrl}"
           aria-current="page">

            <svg viewBox="0 0 24 24"
                 aria-hidden="true"
                 fill="none"
                 stroke="currentColor"
                 stroke-width="1.7"
                 stroke-linecap="round"
                 stroke-linejoin="round">

                <path d="m5 12 4 4L19 6"/>

            </svg>

            <span>全体の安否確認</span>
        </a>

        <a class="nav-item"
           href="${pageContext.request.contextPath}/safety/principal/Distribution.action">

            <svg viewBox="0 0 24 24"
                 aria-hidden="true"
                 fill="none"
                 stroke="currentColor"
                 stroke-width="1.7"
                 stroke-linecap="round"
                 stroke-linejoin="round">

                <path d="m12 3 10 18H2ZM12 9v5m0 3v.1"/>

            </svg>

            <span>通知配信</span>
        </a>

        <a class="nav-item"
           href="${pageContext.request.contextPath}/safety/principal/Groups.action">

            <svg viewBox="0 0 24 24"
                 aria-hidden="true"
                 fill="none"
                 stroke="currentColor"
                 stroke-width="1.7"
                 stroke-linecap="round"
                 stroke-linejoin="round">

                <circle cx="9" cy="8" r="3"/>
                <path d="M3 20v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 5"/>

            </svg>

            <span>グループ管理</span>
        </a>

        <a class="nav-item"
           href="${pageContext.request.contextPath}/safety/principal/Staff.action">

            <svg viewBox="0 0 24 24"
                 aria-hidden="true"
                 fill="none"
                 stroke="currentColor"
                 stroke-width="1.7"
                 stroke-linecap="round"
                 stroke-linejoin="round">

                <circle cx="9" cy="8" r="3"/>
                <path d="M3 20v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 5"/>

            </svg>

            <span>職員管理</span>
        </a>

        <a class="nav-item"
           href="${pageContext.request.contextPath}/safety/principal/DangerTypes.action">

            <svg viewBox="0 0 24 24"
                 aria-hidden="true"
                 fill="none"
                 stroke="currentColor"
                 stroke-width="1.7"
                 stroke-linecap="round"
                 stroke-linejoin="round">

                <path d="m12 3 10 18H2ZM12 9v5m0 3v.1"/>

            </svg>

            <span>困っている内容</span>
        </a>

        <footer class="app-footer portal-footer">
            ＠AnpiLink
        </footer>

    </aside>

    <main class="portal-main">

        <nav class="safety-breadcrumb"
             aria-label="現在の位置">

            <a href="${answersUrl}">
                全体の安否確認
            </a>

            <span aria-hidden="true">›</span>

            <span aria-current="page">
                <c:out value="${grade}" />年生
            </span>

        </nav>

        <div class="class-selection-heading">

            <h1>
                <c:out value="${grade}" />年生の安否確認
            </h1>

            <a class="class-back-link"
               href="${answersUrl}">
                ← 学年選択に戻る
            </a>

        </div>

        <section class="class-selection-panel" aria-labelledby="class-selection-title">

            <h2 id="class-selection-title">
                クラス別の回答状況
            </h2>

            <div class="class-selection-scroll">

                <table class="class-selection-table">

                    <thead>
                        <tr>
                            <th scope="col">クラス</th>
                            <th scope="col">生徒数</th>
                            <th scope="col">要対応</th>
                            <th scope="col"><span class="selection-sr-only">生徒一覧</span></th>
                        </tr>
                    </thead>

                    <tbody>

                    <c:choose>

                        <c:when test="${empty classes}">
                            <tr>
                                <td colspan="4"
                                    class="muted">
                                    対象のクラスはありません。
                                </td>
                            </tr>
                        </c:when>

                        <c:otherwise>

                            <c:forEach var="item"
                                       items="${classes}">

                                <tr>

                                    <th scope="row">
                                        <c:out value="${item.className}" />
                                    </th>

                                    <td>
                                        <span class="class-metric">
                                            <c:out value="${item.studentCount}" />
                                        </span>

                                        <span class="class-unit">
                                            人
                                        </span>
                                    </td>

                                    <td>

                                        <span class="class-danger-count ${item.attentionCount > 0 ? 'has-danger' : 'no-danger'}">

                                            <c:out value="${item.attentionCount}" />
                                            <span>人</span>

                                        </span>

                                    </td>

                                    <td>

                                        <a class="class-student-link"
                                           href="${studentsUrl}?classId=${item.classId}&amp;schoolYear=${item.schoolYear}">

                                            生徒を見る
                                            <span aria-hidden="true">→</span>

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

    </main>

</div>

<div class="modal"
     id="logout-dialog"
     role="dialog"
     aria-modal="true"
     aria-labelledby="logout-title"
     aria-hidden="true">

    <div class="dialog">

        <h2 id="logout-title">
            ログアウトしますか？
        </h2>

        <form action="${logoutUrl}"
              method="post">

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

<script>
const logoutModal =
        document.getElementById("logout-dialog");

function openLogoutDialog() {
    logoutModal.classList.add("show");
    logoutModal.setAttribute("aria-hidden", "false");
}

function closeLogoutDialog() {
    logoutModal.classList.remove("show");
    logoutModal.setAttribute("aria-hidden", "true");
}

document.addEventListener(
        "keydown",
        function(event) {

            if (event.key === "Escape") {
                closeLogoutDialog();
            }
        });
</script>

</body>
</html>
