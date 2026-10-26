<%-- 生徒の通信状態画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">

<title>通信状態 | AnpiLink</title>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/style.css?v=20261014-1">
</head>
<body class="a">
	<%-- 遷移先URL --%>
	<c:url var="homeUrl" value="/safety/student/Home.action" />
	<c:url var="moreUrl" value="/safety/student/More.action" />

	<c:url var="connectionCheckUrl"
		value="/safety/student/Connection.action">
		<c:param name="check" value="true" />
	</c:url>

	<c:url var="loginUrl" value="/safety/auth/Login.action" />

	<div class="phone">
		<header class="phone-header">
			<div class="brand">AnpiLink</div>
		</header>

		<main class="page">
			<div class="mobile-page-heading">
				<h1>通信状態</h1>

				<a class="mobile-back-button" href="${moreUrl}"> ← 戻る </a>
			</div>

			<div class="card" id="connection-status" role="status"
				aria-live="polite"
				data-check-url="<c:out value='${connectionCheckUrl}' />">

				<b id="connection-label">未確認</b>

				<p id="connection-description">JavaScriptを有効にすると、接続状態を確認できます。</p>
			</div>

			<button type="button" class="primary group-submit"
				id="connection-recheck" disabled>再確認</button>

			<a class="parent-settings-return" id="connection-login"
				href="${loginUrl}" hidden> ログイン画面へ </a>

			<p class="muted">サーバーとの接続を確認します。 安否回答の送信完了を示すものではありません。</p>

			<p class="muted">オフライン時の回答の一時保存・再送信機能は準備中です。</p>
		</main>

		<footer class="app-footer mobile-footer"> ＠AnpiLink </footer>

		<nav class="phone-tabs tabs two" aria-label="メインメニュー">
			<a href="${homeUrl}"> <svg viewBox="0 0 24 24" aria-hidden="true"
					fill="none" stroke="currentColor" stroke-width="1.7"
					stroke-linecap="round" stroke-linejoin="round">
                    <path d="m3 10 9-7 9 7M5 9v12h14V9M9 21v-7h6v7" />
                </svg> <span>ホーム</span>
			</a> <a href="${moreUrl}"> <svg viewBox="0 0 24 24"
					aria-hidden="true" fill="none" stroke="currentColor"
					stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M5 6h14M5 12h14M5 18h14" />
                </svg> <span>その他</span>
			</a>
		</nav>
	</div>
	<script>
(function () {
    "use strict";

    var statusCard = document.getElementById("connection-status");
    var label = document.getElementById("connection-label");
    var description =
        document.getElementById("connection-description");
    var recheck = document.getElementById("connection-recheck");
    var loginLink = document.getElementById("connection-login");

    var checkUrl = statusCard.getAttribute("data-check-url");
    var checking = false;

    // 確認結果を表示する
    function display(title, text, loginRequired) {
        label.textContent = title;
        description.textContent = text;
        loginLink.hidden = !loginRequired;
    }

    // アプリケーションサーバーへの接続を確認する
    function checkConnection() {
        if (checking) {
            return;
        }

        checking = true;
        recheck.disabled = true;

        display(
            "確認中",
            "サーバーとの接続を確認しています。",
            false
        );

        var controller = new AbortController();

        // 応答がない場合は8秒で確認を終了する
        var timeoutId = window.setTimeout(function () {
            controller.abort();
        }, 8000);

        fetch(checkUrl, {
            method: "GET",
            credentials: "same-origin",
            cache: "no-store",
            redirect: "error",
            signal: controller.signal
        })
        .then(function (response) {
            if (response.status === 204) {
                display(
                    "接続確認済み",
                    "サーバーと通信できました。",
                    false
                );

            } else if (response.status === 401) {
                display(
                    "ログインが必要です",
                    "サーバーに接続できましたが、" +
                    "ログインの有効期限が切れています。" +
                    "ログインし直してください。",
                    true
                );

            } else if (response.status === 403) {
                display(
                    "利用できません",
                    "このアカウントでは接続確認を利用できません。",
                    true
                );

            } else {
                display(
                    "サーバーエラー",
                    "サーバーからエラーが返されました。" +
                    "時間をおいて再確認してください。" +
                    "（HTTP " + response.status + "）",
                    false
                );
            }
        })
        .catch(function () {
            // 通信失敗だけで原因を断定しない
            if (navigator.onLine === false) {
                display(
                    "オフライン",
                    "端末がオフラインと判定しています。" +
                    "通信環境を確認して再確認してください。",
                    false
                );
            } else {
                display(
                    "接続を確認できません",
                    "通信環境またはサーバーの状態により、" +
                    "接続を確認できませんでした。",
                    false
                );
            }
        })
        .then(function () {
            window.clearTimeout(timeoutId);
            checking = false;
            recheck.disabled = false;
        });
    }

    recheck.addEventListener("click", checkConnection);

    // 通信状態の変化をきっかけに再確認する
    window.addEventListener("online", checkConnection);
    window.addEventListener("offline", checkConnection);

    checkConnection();
})();
</script>
</body>
</html>