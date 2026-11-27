<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:url var="homeUrl" value="/safety/principal/Home.action" />
<c:url var="answersUrl" value="/safety/principal/Answers.action" />
<c:url var="distributionUrl" value="/safety/principal/Distribution.action" />
<c:url var="submitUrl" value="/safety/principal/DistributionSubmit.action" />
<c:url var="logoutUrl" value="/safety/auth/Logout.action" />
<!doctype html>
<html lang="ja">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>通知配信 | AnpiLink</title>
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
        <a class="nav-item active" href="${distributionUrl}">
          <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="m12 3 10 18H2ZM12 9v5m0 3v.1"/>
          </svg>
          <span>通知配信</span>
        </a>
        <a class="nav-item " href="${pageContext.request.contextPath}/safety/principal/Groups.action">
          <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="8" r="3"/>
            <path d="M3 20v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 5"/>
          </svg>
          <span>グループ管理</span>
        </a>
        <a class="nav-item " href="${pageContext.request.contextPath}/safety/principal/Staff.action">
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
        <h1>通知配信</h1>
        <form class="card form" action="${submitUrl}" method="post">
          <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />">
          <c:if test="${not empty dispatchError}">
            <p role="alert"><c:out value="${dispatchError}" /></p>
          </c:if>
          <label for="dispatch-kind">配信種類</label>
          <select id="dispatch-kind" name="kind" required>
            <option value="SAFETY" ${kind != 'NORMAL' ? 'selected' : ''}>安否確認通知</option>
            <option value="NORMAL" ${kind == 'NORMAL' ? 'selected' : ''}>通常通知</option>
          </select>
          <label for="target">対象</label>
          <select id="target" name="target" required>
            <option value="STUDENT" ${target == 'STUDENT' ? 'selected' : ''}>全生徒</option>
            <option value="FAMILY" ${target == 'FAMILY' ? 'selected' : ''}>全保護者</option>
            <option value="TEACHER" ${target == 'TEACHER' ? 'selected' : ''}>全職員</option>
            <option value="ALL" ${target == 'ALL' ? 'selected' : ''}>全生徒・全保護者・全職員</option>
          </select>
          <div id="safety-fields">
            <label for="disaster">災害の種類</label>
            <select id="disaster" name="disaster" required>
              <option value="EARTHQUAKE" ${disaster == 'EARTHQUAKE' ? 'selected' : ''}>地震</option>
              <option value="TYPHOON" ${disaster == 'TYPHOON' ? 'selected' : ''}>台風</option>
              <option value="FLOOD" ${disaster == 'FLOOD' ? 'selected' : ''}>大雨・洪水</option>
              <option value="TSUNAMI" ${disaster == 'TSUNAMI' ? 'selected' : ''}>津波</option>
              <option value="FIRE" ${disaster == 'FIRE' ? 'selected' : ''}>火災</option>
              <option value="LANDSLIDE" ${disaster == 'LANDSLIDE' ? 'selected' : ''}>土砂災害</option>
              <option value="OTHER" ${disaster == 'OTHER' ? 'selected' : ''}>その他</option>
            </select>
            <label>配信内容プレビュー</label>
            <div class="card" aria-live="polite">
              <strong id="preview-title"></strong>
              <p id="preview-message"></p>
            </div>
          <label for="answer-deadline">回答期限</label>
          <input id="answer-deadline" name="deadline" value="<c:out value='${deadline}' />" type="datetime-local" required>
          <p class="muted">期限を過ぎても未回答の場合、保護者のアプリに催促通知を送ります。</p>
          </div>
          <div id="normal-fields" hidden>
          <label for="distribution-title">タイトル</label>
          <input id="distribution-title" name="title" value="<c:out value='${dispatchTitle}' />" type="text" maxlength="100" required>
          <label for="distribution-message">メッセージ</label>
          <textarea id="distribution-message" name="message" rows="6" maxlength="2000" required><c:out value="${dispatchMessage}" /></textarea>
          </div>
          <button class="primary group-submit" type="submit">配信する</button>
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

const kindSelect = document.getElementById("dispatch-kind");
const targetSelect = document.getElementById("target");
const disasterSelect = document.getElementById("disaster");
const safetyFields = document.getElementById("safety-fields");
const normalFields = document.getElementById("normal-fields");
function updatePreview() {
    const label = disasterSelect.options[disasterSelect.selectedIndex].text;
    document.getElementById("preview-title").textContent = label === "その他" ? "緊急時の安否確認" : label + "に伴う安否確認";
    document.getElementById("preview-message").textContent = (label === "その他" ? "緊急事態に伴い、" : label + "に伴い、") + "現在の安否状況を回答してください。";
}
function updateKind() {
    const safety = kindSelect.value === "SAFETY";
    if (safety) targetSelect.value = "STUDENT";
    targetSelect.disabled = safety;
    safetyFields.hidden = !safety;
    normalFields.hidden = safety;
    safetyFields.querySelectorAll("input,select").forEach(el => { el.disabled = !safety; });
    normalFields.querySelectorAll("input,textarea").forEach(el => { el.disabled = safety; });
    updatePreview();
}
kindSelect.addEventListener("change", updateKind);
disasterSelect.addEventListener("change", updatePreview);
updateKind();

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
