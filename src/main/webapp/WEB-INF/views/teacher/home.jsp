<%-- 教員ホーム画面 --%>
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

    <title>教員ホーム | 学校安否確認</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <c:url var="homeUrl"
           value="/safety/teacher/Home.action" />
     
    <c:url var="logoutUrl"
           value="/safety/auth/Logout.action" />

    <header class="stafftop">
        <div class="brand">学校安否確認</div>

        <div class="header-user">
            <span>
                <c:out value="${user.name}" />先生
            </span>

            <button type="button"
                    class="logout"
                    onclick="openLogoutDialog()">
                ログアウト
            </button>
        </div>
    </header>

    <div class="stafflayout">
        
        <aside class="side">
    <a class="on"
       href="${homeUrl}"
       aria-current="page">
        ホーム
    </a>

    <%-- 遷移先は未実装のため、リンク先を設定しない --%>
    <a aria-disabled="true">回答状況</a>
    <a aria-disabled="true">安否確認を配信</a>
    <a aria-disabled="true">回答履歴</a>
</aside>

        <main class="main">
            <h1>教員ホーム</h1>

            <div class="stats">
                <div class="stat">
                    <small>回答済み</small>
                    <strong>—</strong>
                </div>

                <div class="stat">
                    <small>未回答</small>
                    <strong>—</strong>
                </div>

                <div class="stat">
                    <small>危険</small>
                    <strong style="color: #c94747">—</strong>
                </div>
            </div>

            <p class="muted">
                集計機能は準備中です。
            </p>

            <section class="alarm">
                <div class="alarm-title">
                    ⚠ 危険状態の生徒
                </div>

                <p class="muted">
                    危険状態の生徒を表示する機能は準備中です。
                </p>
            </section>

            <section class="table">
                <div class="row">
                    <b>最新の安否回答</b>
                </div>

                <div class="row">
                    <span class="muted">
                        安否回答を表示する機能は準備中です。
                    </span>
                </div>
            </section>
        </main>
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