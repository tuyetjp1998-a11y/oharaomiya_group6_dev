<%-- 校長ホーム画面 --%>
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

    <title>管理ホーム | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css?v=20261015-2">
</head>
<body class="a">
    <%-- 遷移先URL --%>
    <c:url var="homeUrl"
           value="/safety/principal/Home.action" />
    <c:url var="logoutUrl"
           value="/safety/auth/Logout.action" />

    <header class="portal-header">
        <div class="brand">AnpiLink</div>

        <div class="portal-user">
            <span class="user-dot" aria-hidden="true">校</span>

            <span>
                <c:out value="${sessionScope.user.name}" />校長
            </span>

            <button type="button"
                    class="logout"
                    onclick="openLogoutDialog()">
                ログアウト
            </button>
        </div>
    </header>

    <div class="portal-layout">
        <aside class="portal-nav">
            <span class="nav-caption">管理ポータル</span>

            <a class="nav-item active"
               href="${homeUrl}"
               aria-current="page">
                <svg viewBox="0 0 24 24"
                     aria-hidden="true"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="1.7"
                     stroke-linecap="round"
                     stroke-linejoin="round">
                    <path d="m3 10 9-7 9 7M5 9v12h14V9M9 21v-7h6v7" />
                </svg>
                <span>ホーム</span>
            </a>

            <%-- 未実装のメニュー --%>
            <a class="nav-item"
               role="link"
               aria-disabled="true">
                <svg viewBox="0 0 24 24"
                     aria-hidden="true"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="1.7"
                     stroke-linecap="round"
                     stroke-linejoin="round">
                    <path d="m5 12 4 4L19 6" />
                </svg>
                <span>全体の安否確認</span>
            </a>

            <a class="nav-item"
               role="link"
               aria-disabled="true">
                <svg viewBox="0 0 24 24"
                     aria-hidden="true"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="1.7"
                     stroke-linecap="round"
                     stroke-linejoin="round">
                    <path d="m12 3 10 18H2ZM12 9v5m0 3v.1" />
                </svg>
                <span>安否確認（災害時）</span>
            </a>

            <a class="nav-item"
               role="link"
               aria-disabled="true">
                <svg viewBox="0 0 24 24"
                     aria-hidden="true"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="1.7"
                     stroke-linecap="round"
                     stroke-linejoin="round">
                    <circle cx="9" cy="8" r="3" />
                    <path d="M3 20v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 5" />
                </svg>
                <span>グループ管理</span>
            </a>

            <a class="nav-item"
               role="link"
               aria-disabled="true">
                <svg viewBox="0 0 24 24"
                     aria-hidden="true"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="1.7"
                     stroke-linecap="round"
                     stroke-linejoin="round">
                    <circle cx="9" cy="8" r="3" />
                    <path d="M3 20v-3a6 6 0 0 1 12 0v3M17 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 5" />
                </svg>
                <span>教員管理</span>
            </a>

            <a class="nav-item"
               role="link"
               aria-disabled="true">
                <svg viewBox="0 0 24 24"
                     aria-hidden="true"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="1.7"
                     stroke-linecap="round"
                     stroke-linejoin="round">
                    <path d="M3 11a9 9 0 1 1 2 7M3 4v7h7M12 7v6l4 2" />
                </svg>
                <span>回答履歴</span>
            </a>

            <footer class="app-footer portal-footer">
                ＠AnpiLink
            </footer>
        </aside>

        <main class="portal-main">
            <div class="portal-heading">
                <div>
                    <p class="eyebrow">管理ホーム</p>
                    <h1>全体の安否状況</h1>
                </div>

                <div class="portal-date">
                    —
                    <span>最終回答：未取得</span>
                </div>
            </div>

            <section class="stats">
                <div class="stat">
                    <span>回答済み</span>

                    <div>
                        <strong>—</strong>
                        <small>人</small>
                    </div>

                    <p>集計機能は準備中です。</p>
                </div>

                <div class="stat">
                    <span>未回答</span>

                    <div>
                        <strong>—</strong>
                        <small>人</small>
                    </div>

                    <p>集計機能は準備中です。</p>
                </div>

                <div class="stat danger-stat">
                    <span>危険</span>

                    <div>
                        <strong>—</strong>
                        <small>人</small>
                    </div>

                    <p>集計機能は準備中です。</p>
                </div>
            </section>

            <div class="portal-content">
                <section class="alarm-panel">
                    <div class="section-heading">
                        <h2>
                            <svg viewBox="0 0 24 24"
                                 aria-hidden="true"
                                 fill="none"
                                 stroke="currentColor"
                                 stroke-width="1.7"
                                 stroke-linecap="round"
                                 stroke-linejoin="round">
                                <path d="m12 3 10 18H2ZM12 9v5m0 3v.1" />
                            </svg>
                            危険状態の生徒
                        </h2>

                        <span class="count">— 人</span>
                    </div>

                    <div class="danger-list">
                        <p class="muted">
                            危険状態の生徒を表示する機能は準備中です。
                        </p>
                    </div>
                </section>

                <section class="answers-panel">
                    <div class="section-heading">
                        <h2>最新の安否回答</h2>
                    </div>

                    <div class="table-scroll">
                        <table>
                            <thead>
                                <tr>
                                    <th scope="col">生徒</th>
                                    <th scope="col">クラス</th>
                                    <th scope="col">安否</th>
                                    <th scope="col">回答時刻</th>
                                    <th scope="col">詳細</th>
                                </tr>
                            </thead>

                            <tbody>
                                <tr>
                                    <td colspan="5" class="muted">
                                        安否回答を表示する機能は準備中です。
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </section>
            </div>
        </main>
    </div>

    <%-- ログアウト確認ダイアログ --%>
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
                            id="logoutCancel"
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
        const logoutModal =
                document.getElementById("logout-dialog");

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
                const buttons =
                        logoutModal.querySelectorAll("button");

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