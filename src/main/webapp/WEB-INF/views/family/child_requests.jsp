<%-- 保護者の申請履歴画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">

<title>申請履歴 | AnpiLink</title>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="a">
	<c:url var="homeUrl" value="/safety/family/Home.action" />
	<c:url var="moreUrl" value="/safety/family/More.action" />
	<c:url var="settingsUrl" value="/safety/family/Settings.action" />

	<div class="phone">
		<header class="phone-header">
			<div class="brand">AnpiLink</div>
		</header>

		<main class="page">
			<a class="back" href="${settingsUrl}">← 設定</a>

			<h1>申請履歴</h1>

			<c:if test="${not empty requestScope.childRequestMessage}">
				<p role="status">
					<c:out value="${requestScope.childRequestMessage}" />
				</p>
			</c:if>

			<div id="parent-request-history" class="parent-request-history">

				<c:choose>
					<c:when test="${empty requestScope.requests}">
						<div class="card">
							<p class="muted">申請履歴はありません。</p>
						</div>
					</c:when>

					<c:otherwise>
						<c:forEach var="item" items="${requestScope.requests}">
							<section class="card link-request-card">
								<div class="request-heading">
									<b> 生徒ID： <c:out value="${item.studentLoginId}" />
									</b>


									<c:choose>
										<c:when test="${item.status eq 'PENDING'}">
											<span class="status-badge request-pending"> 承認待ち </span>
										</c:when>

										<c:when test="${item.status eq 'APPROVED'}">
											<span class="status-badge safe"> 承認済み </span>
										</c:when>

										<c:when test="${item.status eq 'REJECTED'}">
											<span class="status-badge request-rejected"> 却下 </span>
										</c:when>
									</c:choose>
								</div>
								<p class="parent-request-date">
									申請日：
									<c:out value="${item.requestedAtText}" />
								</p>

								<c:choose>
									<c:when test="${item.status eq 'PENDING'}">
										<p>教員の承認をお待ちください。</p>
									</c:when>

									<c:when test="${item.status eq 'REJECTED'}">
										<p>親子関係を確認できませんでした。 学校へお問い合わせください。</p>
									</c:when>
								</c:choose>
							</section>
						</c:forEach>
					</c:otherwise>
				</c:choose>
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
</body>
</html>