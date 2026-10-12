<%-- 保護者ホーム画面 --%>
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

    <title>保護者ホーム | 学校安否確認</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <c:url var="homeUrl"
           value="/safety/family/Home.action" />
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
                <div class="who">保護者</div>

                <h1>
                    <c:out value="${sessionScope.user.name}" />さん
                </h1>

                <%-- 保護者の回答画面は未接続 --%>
                <a class="answer" aria-disabled="true">
                    安否を回答
                </a>
            </div>
        </main>

        <nav class="tabs" aria-label="メインメニュー">
            <a href="${homeUrl}" aria-current="page">
                <b aria-hidden="true">●</b>
                ホーム
            </a>

            <a aria-disabled="true">
                <b aria-hidden="true">人</b>
                生徒回答
            </a>

            <a aria-disabled="true">
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