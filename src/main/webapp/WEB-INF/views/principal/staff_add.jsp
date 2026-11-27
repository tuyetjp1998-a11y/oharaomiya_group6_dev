<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:url var="homeUrl" value="/safety/principal/Home.action" />
<c:url var="answersUrl" value="/safety/principal/Answers.action" />
<c:url var="distributionUrl" value="/safety/principal/Distribution.action" />
<c:url var="groupsUrl" value="/safety/principal/Groups.action" />
<c:url var="assignUrl" value="/safety/principal/GroupAssign.action" />
<c:url var="createUrl" value="/safety/principal/GroupCreate.action" />
<c:url var="logoutUrl" value="/safety/auth/Logout.action" />
<!doctype html>
<html lang="ja">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>職員アカウント登録 | AnpiLink</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=20261124-3">
  </head>
  <body class="a admin-portal">
    <header class="portal-header">
      <div class="brand">
        AnpiLink
      </div>
      <div class="portal-user">
        <span class="user-dot">校</span><span>校長</span>
        <button type="button" class="logout" onclick="openLogoutDialog()">ログアウト</button>
      </div>
    </header>
    <div class="portal-layout">
      <aside class="portal-nav">
        <span class="nav-caption">管理ポータル</span>
        <a class="nav-item " href="${homeUrl}">
          <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="m3 10 9-7 9 7M5 9v12h14V9M9 21v-7h6v7"/>
          </svg>
          <span>ホーム</span>
        </a>
        <a class="nav-item " href="${answersUrl}">
          <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="m5 12 4 4L19 6"/>
          </svg>
          <span>全体の安否確認</span>
        </a>
        <a class="nav-item " href="${distributionUrl}">
          <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="m12 3 10 18H2ZM12 9v5m0 3v.1"/>
          </svg>
          <span>通知配信</span>
        </a>
        <a class="nav-item" href="${pageContext.request.contextPath}/safety/principal/Groups.action">
          <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="8" r="3"/>
            <path d="M3 20v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 5"/>
          </svg>
          <span>グループ管理</span>
        </a>
        <a class="nav-item active" href="${pageContext.request.contextPath}/safety/principal/Staff.action" aria-current="page">
          <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="8" r="3"/>
            <path d="M3 20v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 5"/>
          </svg>
          <span>職員管理</span>
        </a>
        <a class="nav-item " href="${pageContext.request.contextPath}/safety/principal/DangerTypes.action">
          <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="m12 3 10 18H2ZM12 9v5m0 3v.1"/>
          </svg>
          <span>困っている内容</span>
        </a>
        <footer class="app-footer portal-footer">＠AnpiLink</footer>
      </aside>
            <main class="portal-main">
        <h1>職員アカウント登録</h1>
        <a class="back" href="${pageContext.request.contextPath}/safety/principal/Staff.action">← 職員管理</a>
        <form class="card form" action="${pageContext.request.contextPath}/safety/principal/StaffAddSubmit.action" method="post">
          <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />">
          <c:if test="${not empty staffError}"><p role="alert" style="color:#b33a48"><c:out value="${staffError}" /></p></c:if>
          <label for="name">氏名</label>
          <input id="name" name="name" required maxlength="40" value="<c:out value='${staffName}' />">
          <label for="id">ユーザーID</label>
          <input id="id" name="loginId" required maxlength="40" autocomplete="off" value="<c:out value='${staffLoginId}' />">
          <label for="password">初期パスワード</label>
          <input id="password" name="password" type="password" required autocomplete="new-password" minlength="8">
          <button type="submit" class="primary group-submit">登録する</button>
        </form>
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
