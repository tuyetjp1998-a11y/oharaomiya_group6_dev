<%-- 生徒ホーム画面 --%>
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

    <title>生徒ホーム | 学校安否確認</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <%-- 遷移先URL --%>
    <c:url var="homeUrl"
           value="/safety/student/Home.action" />
    <c:url var="answerUrl"
           value="/safety/student/Answer.action" />
    <c:url var="parentStatusUrl"
           value="/safety/student/ParentStatus.action" />
    <c:url var="moreUrl"
           value="/safety/student/More.action" />
    <c:url var="logoutUrl"
           value="/safety/auth/Logout.action" />

    <div class="phone">
        <header class="top">
            <div class="brand">学校安否確認</div>

            <button type="button"
                    class="logout"
                    onclick="openLogoutDialog()">
                ログアウト
            </button>
        </header>

        <main class="home-only">
            <div class="home-box">
                <div class="who">生徒</div>

                <h1>
                    <c:out value="${sessionScope.user.name}" />さん
                </h1>

                <a class="answer" href="${answerUrl}">
                    安否を回答
                </a>
            </div>
        </main>

        <nav class="tabs" aria-label="メインメニュー">
            <a href="${homeUrl}" aria-current="page">
                <b aria-hidden="true">●</b>
                ホーム
            </a>

            <a href="${parentStatusUrl}">
                <b aria-hidden="true">人</b>
                保護者回答
            </a>

            <a href="${moreUrl}">
                <b aria-hidden="true">≡</b>
                その他
            </a>
        </nav>
    </div>

    <%-- ログアウト確認ダイアログ --%>
    <div class="modal"
         id="logoutModal"
         role="dialog"
         aria-modal="true"
         aria-labelledby="logoutTitle"
         aria-hidden="true">

        <div class="dialog">
            <h2 id="logoutTitle">ログアウトしますか？</h2>

            <form action="${logoutUrl}" method="post">
                <input type="hidden"
                       name="csrfToken"
                       value="${sessionScope.csrfToken}">

                <div>
                    <button type="button"
                            id="logoutCancel"
                            onclick="closeLogoutDialog()">
                        キャンセル
                    </button>

                    <button type="submit"
                            style="background: #c94747; color: white;">
                        ログアウト
                    </button>
                </div>
            </form>
        </div>
    </div>

    <script>
        const logoutModal = document.getElementById("logoutModal");
        let previousFocus = null;

        function openLogoutDialog() {
            previousFocus = document.activeElement;
            logoutModal.classList.add("show");
            logoutModal.setAttribute("aria-hidden", "false");
            document.getElementById("logoutCancel").focus();
        }

        function closeLogoutDialog() {
            logoutModal.classList.remove("show");
            logoutModal.setAttribute("aria-hidden", "true");

            if (previousFocus) {
                previousFocus.focus();
            }
        }

        document.addEventListener("keydown", function (event) {
            if (!logoutModal.classList.contains("show")) {
                return;
            }

            if (event.key === "Escape") {
                closeLogoutDialog();
            }

            if (event.key === "Tab") {
                const buttons = logoutModal.querySelectorAll("button");
                const firstButton = buttons[0];
                const lastButton = buttons[buttons.length - 1];

                if (event.shiftKey &&
                    document.activeElement === firstButton) {
                    event.preventDefault();
                    lastButton.focus();
                } else if (!event.shiftKey &&
                           document.activeElement === lastButton) {
                    event.preventDefault();
                    firstButton.focus();
                }
            }
        });
    </script>
</body>
</html>