<%-- 保護者のその他画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">

<title>その他 | AnpiLink</title>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="a">
	<c:url var="homeUrl" value="/safety/family/Home.action" />
	<c:url var="moreUrl" value="/safety/family/More.action" />
	<c:url var="logoutUrl" value="/safety/auth/Logout.action" />
	<c:url var="noticesUrl" value="/safety/family/Notices.action" />
	<c:url var="settingsUrl" value="/safety/family/Settings.action" />

	<div class="phone">
		<header class="phone-header">
			<div class="brand">AnpiLink</div>
		</header>

		<main class="page">
			<h1>その他</h1>

			<div class="menu">
				<%-- 各画面は次の段階で連携する --%>
				<a href="${noticesUrl}"> <b>学校からの連絡</b> <span aria-hidden="true">›</span>
				</a> 
				
				<a href="${settingsUrl}"> <b>設定</b> <span aria-hidden="true">›</span>
				</a>

				<button type="button" class="logout-menu"
					onclick="openLogoutDialog()">
					<b>ログアウト</b> <span aria-hidden="true">›</span>
				</button>
			</div>
		</main>

		<footer class="app-footer mobile-footer"> ＠AnpiLink </footer>

		<nav class="phone-tabs tabs two" aria-label="メインメニュー">
			<a href="${homeUrl}"> <svg viewBox="0 0 24 24" aria-hidden="true"
					fill="none" stroke="currentColor" stroke-width="1.7"
					stroke-linecap="round" stroke-linejoin="round">
                    <path d="m3 10 9-7 9 7M5 9v12h14V9M9 21v-7h6v7" />
                </svg> <span>ホーム</span>
			</a> <a class="active" href="${moreUrl}" aria-current="page"> <svg
					viewBox="0 0 24 24" aria-hidden="true" fill="none"
					stroke="currentColor" stroke-width="1.7" stroke-linecap="round"
					stroke-linejoin="round">
                    <path d="M5 6h14M5 12h14M5 18h14" />
                </svg> <span>その他</span>
			</a>
		</nav>
	</div>

	<%-- ログアウト確認ダイアログ --%>
	<div class="modal" id="logout-dialog" role="dialog" aria-modal="true"
		aria-labelledby="logout-title" aria-hidden="true">
		<div class="dialog">
			<h2 id="logout-title">ログアウトしますか？</h2>

			<form action="${logoutUrl}" method="post">
				<input type="hidden" name="csrfToken"
					value="${sessionScope.csrfToken}">

				<div>
					<button type="button" class="plain-button" id="logoutCancel"
						onclick="closeLogoutDialog()">キャンセル</button>

					<button type="submit" class="delete-button">ログアウト</button>
				</div>
			</form>
		</div>
	</div>

	<script>
		const logoutModal = document.getElementById("logout-dialog");
		const phone = document.querySelector(".phone");
		let previousFocus = null;

		function openLogoutDialog() {
			previousFocus = document.activeElement;

			phone.inert = true;
			logoutModal.classList.add("show");
			logoutModal.setAttribute("aria-hidden", "false");

			document.getElementById("logoutCancel").focus();
		}

		function closeLogoutDialog() {
			phone.inert = false;

			if (previousFocus) {
				previousFocus.focus();
			}

			logoutModal.classList.remove("show");
			logoutModal.setAttribute("aria-hidden", "true");
		}

		document.addEventListener("keydown", function(event) {
			if (!logoutModal.classList.contains("show")) {
				return;
			}

			if (event.key === "Escape") {
				event.preventDefault();
				closeLogoutDialog();
			}

			if (event.key === "Tab") {
				const buttons = logoutModal.querySelectorAll("button");
				const firstButton = buttons[0];
				const lastButton = buttons[buttons.length - 1];

				if (event.shiftKey && document.activeElement === firstButton) {
					event.preventDefault();
					lastButton.focus();
				} else if (!event.shiftKey
						&& document.activeElement === lastButton) {
					event.preventDefault();
					firstButton.focus();
				}
			}
		});
	</script>
</body>
</html>