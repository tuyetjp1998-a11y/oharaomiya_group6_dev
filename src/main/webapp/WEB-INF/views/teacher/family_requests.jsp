<%-- 親子関係の承認画面 --%>
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

    <title>親子関係の承認 | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <c:url var="homeUrl"
           value="/safety/teacher/Home.action" />
    <c:url var="requestsUrl"
           value="/safety/teacher/FamilyRequests.action" />
    <c:url var="reviewUrl"
           value="/safety/teacher/FamilyRequestReview.action" />
    <c:url var="logoutUrl"
           value="/safety/auth/Logout.action" />

    <header class="stafftop">
        <div class="brand">AnpiLink</div>

        <div class="header-user">
            <span>
                <c:out value="${sessionScope.user.name}" />先生
            </span>

            <form action="${logoutUrl}"
                  method="post"
                  onsubmit="return confirm('ログアウトしますか？');">
                <input type="hidden"
                       name="csrfToken"
                       value="${sessionScope.csrfToken}">

                <button type="submit" class="logout">
                    ログアウト
                </button>
            </form>
        </div>
    </header>

    <div class="stafflayout">
        <aside class="side">
            <a href="${homeUrl}">ホーム</a>

            <a class="on"
               href="${requestsUrl}"
               aria-current="page">
                親子関係の承認
            </a>
        </aside>

        <main class="main">
            <h1>親子関係の承認</h1>

            <p class="muted">
                <c:out value="${requestScope.schoolYear}" />年度の
                担当クラスに所属する生徒の申請を表示しています。
            </p>

            <p>
                親子関係を確認したうえで、承認してください。
            </p>

            <c:if test="${not empty requestScope.reviewMessage}">
                <p role="status">
                    <c:out value="${requestScope.reviewMessage}" />
                </p>
            </c:if>

            <c:choose>
                <c:when test="${empty requestScope.requests}">
                    <section class="card">
                        <p class="muted">
                            承認待ちの申請はありません。
                        </p>
                    </section>
                </c:when>

                <c:otherwise>
                    <div class="family-request-scroll">
                        <table class="family-request-table">
                            <caption>承認待ちの申請一覧</caption>

                            <thead>
                                <tr>
                                    <th scope="col">保護者</th>
                                    <th scope="col">生徒</th>
                                    <th scope="col">クラス</th>
                                    <th scope="col">申請日時</th>
                                    <th scope="col">承認・却下</th>
                                </tr>
                            </thead>

                            <tbody>
                                <c:forEach var="item"
                                           items="${requestScope.requests}">
                                    <tr>
                                        <td>
                                            <c:out value="${item.familyName}" />
                                            <small>
                                                <c:out value="${item.familyLoginId}" />
                                            </small>
                                        </td>

                                        <td>
                                            <c:out value="${item.studentName}" />
                                            <small>
                                                <c:out value="${item.studentLoginId}" />
                                            </small>
                                        </td>

                                        <td>
                                            <c:out value="${item.className}" />
                                        </td>

                                        <td>
                                            <c:out value="${item.requestedAtText}" />
                                        </td>

                                        <td>
                                            <form class="family-review-buttons"
                                                  action="${reviewUrl}"
                                                  method="post">

                                                <input type="hidden"
                                                       name="csrfToken"
                                                       value="${sessionScope.csrfToken}">

                                                <input type="hidden"
                                                       name="requestId"
                                                       value="${item.id}">

                                                <button type="submit"
                                                        class="primary"
                                                        name="decision"
                                                        value="APPROVED"
                                                        onclick="return confirm('親子関係を確認しましたか？この申請を承認します。');">
                                                    承認
                                                </button>

                                                <button type="submit"
                                                        class="delete-button"
                                                        name="decision"
                                                        value="REJECTED"
                                                        onclick="return confirm('この申請を却下しますか？');">
                                                    却下
                                                </button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>

            <footer class="app-footer portal-footer">
                ＠AnpiLink
            </footer>
        </main>
    </div>
</body>
</html>