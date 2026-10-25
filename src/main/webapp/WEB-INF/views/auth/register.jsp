<%-- 保護者の新規登録画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">

<title>新規登録 | AnpiLink</title>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="a">
	<c:url var="loginUrl" value="/safety/auth/Login.action" />
	<c:url var="registerExecuteUrl"
		value="/safety/auth/RegisterExecute.action" />

	<main class="login-layout">
		<section class="login-story">
			<h1 class="login-brand">AnpiLink</h1>

			<div class="story-copy">
				<h3 class="story-heading">学校安否確認</h3>
				<h2>
					学校と家庭を、<br>安心でつなぐ。
				</h2>

				<div class="school-art">
					<div class="art-orbit"></div>
					<div class="art-orbit second"></div>

					<div class="art-school">
						<svg viewBox="0 0 24 24" aria-hidden="true" fill="none"
							stroke="currentColor" stroke-width="1.7" stroke-linecap="round"
							stroke-linejoin="round">
                            <path
								d="m3 8 9-5 9 5v13H3ZM9 21v-7h6v7M12 6v4M10 8h4M5 12h2m10 0h2M5 16h2m10 0h2" />
                        </svg>
					</div>

					<span class="art-check"> <svg viewBox="0 0 24 24"
							aria-hidden="true" fill="none" stroke="currentColor"
							stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                            <path d="m5 12 4 4L19 6" />
                        </svg>
					</span>
				</div>
			</div>
		</section>

		<section class="login-form-area">
			<div class="login-form">
				<h2>保護者の新規登録</h2>

				<c:if test="${not empty requestScope.registerError}">
					<p class="form-error" role="alert">
						<c:out value="${requestScope.registerError}" />
					</p>
				</c:if>

				<form class="form" action="${registerExecuteUrl}" method="post">

					<input type="hidden" name="csrfToken"
						value="${sessionScope.csrfToken}"> <label for="name">氏名</label>
					<input id="name" name="name" maxlength="40" autocomplete="name"
						value="<c:out value='${requestScope.inputName}' />" required>

					<label for="id">ユーザーID</label> <input id="id" name="loginId"
						maxlength="40" autocomplete="username"
						value="<c:out value='${requestScope.inputLoginId}' />" required>

					<label for="password">パスワード</label> <input id="password"
						name="password" type="password" autocomplete="new-password"
						required> <label for="child-id">お子さんの生徒ID</label> <input
						id="child-id" name="studentLoginId" maxlength="40"
						autocomplete="off"
						value="<c:out value='${requestScope.inputStudentLoginId}' />"
						required>

					<button class="primary group-submit" type="submit">登録する</button>
				</form>

				<a class="register-link" href="${loginUrl}"> ログインへ </a>
			</div>
		</section>
	</main>
</body>
</html>