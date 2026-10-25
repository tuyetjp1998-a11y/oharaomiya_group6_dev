<%-- 保護者の設定画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">

<title>設定 | AnpiLink</title>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="a">
	<c:url var="homeUrl" value="/safety/family/Home.action" />
	<c:url var="moreUrl" value="/safety/family/More.action" />
	<c:url var="notificationUpdateUrl"
		value="/safety/family/NotificationUpdate.action" />
	<c:url var="childAddUrl" value="/safety/family/ChildAdd.action" />
	<c:url var="childRequestsUrl"
		value="/safety/family/ChildRequests.action" />


	<div class="phone">
		<header class="phone-header">
			<div class="brand">AnpiLink</div>
		</header>

		<main class="page">
			<h1>設定</h1>

			<div class="card">
				<div class="parent-notification-row">
					<span id="parent-notification-label"> 子どもの安否回答を通知 </span>

					<%-- Web Pushとの連携後に有効にする --%>

					<button type="button" id="parent-notification-toggle"
						class="parent-notification-toggle" role="switch"
						aria-checked="${requestScope.notificationEnabled}"
						aria-labelledby="parent-notification-label"
						data-update-url="${notificationUpdateUrl}"
						data-csrf-token="${sessionScope.csrfToken}">


						<span class="parent-notification-knob" aria-hidden="true"></span>

						<span data-notification-state> <c:choose>
								<c:when test="${requestScope.notificationEnabled}">
                        ON
                    </c:when>
								<c:otherwise>
                        OFF
                    </c:otherwise>
							</c:choose>
						</span>
					</button>
				</div>

				<p class="muted" id="notification-message" role="status"
					aria-live="polite">通知設定を変更できます。 ※プッシュ通知の配信機能は準備中です。</p>
			</div>

			<h2>お子さん</h2>

			<div id="parent-children">
				<c:choose>
					<c:when test="${empty requestScope.students}">
						<div class="card">
							<span class="muted"> 関連付けられている生徒はいません。 </span>
						</div>
					</c:when>

					<c:otherwise>
						<c:forEach var="student" items="${requestScope.students}">
							<div class="card child-card">
								<div class="parent-child-name">
									<b> <c:out value="${student.studentName}" />
									</b>
								</div>

								<span class="status-badge safe"> 承認済み </span>
							</div>
						</c:forEach>
					</c:otherwise>
				</c:choose>
			</div>

			<div class="menu parent-settings-menu">
				<a href="${childAddUrl}"> <b>お子さんを追加</b> <span
					aria-hidden="true">›</span>
				</a> <a href="${childRequestsUrl}"> <b>申請履歴</b> <span
					aria-hidden="true">›</span>
				</a>
			</div>
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
	<script src="${pageContext.request.contextPath}/js/family-push.js"
		data-context-path="${pageContext.request.contextPath}"
		data-vapid-public-key="<c:out value='${requestScope.vapidPublicKey}' />">
		
	</script>
</body>
</html>