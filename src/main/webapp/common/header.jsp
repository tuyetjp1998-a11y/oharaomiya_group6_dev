<%-- 共通ヘッダー --%>
<%@ page contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<header class="top">
    <div class="brand">学校安否確認</div>

    <c:if test="${not empty sessionScope.user}">
        <div class="header-user">
            <span>
                <c:out value="${sessionScope.user.name}"/>様
            </span>

            <c:url var="logoutUrl"
                   value="/safety/auth/Logout.action"/>

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
    </c:if>
</header>