<%-- 共通ナビゲーション --%>
<%@ page contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:if test="${not empty sessionScope.user}">
    <nav class="side" aria-label="メインメニュー">
        <c:choose>
            <%-- 生徒 --%>
            <c:when test="${sessionScope.user.role eq 'STUDENT'}">
                <c:url var="studentHomeUrl"
                       value="/safety/student/Home.action"/>

                <a href="${studentHomeUrl}">ホーム</a>
            </c:when>

            <%-- 保護者 --%>
            <c:when test="${sessionScope.user.role eq 'FAMILY'}">
                <c:url var="familyHomeUrl"
                       value="/safety/family/Home.action"/>

                <a href="${familyHomeUrl}">ホーム</a>
            </c:when>

            <%-- 教員 --%>
            <c:when test="${sessionScope.user.role eq 'TEACHER'}">
                <c:url var="teacherHomeUrl"
                       value="/safety/teacher/Home.action"/>

                <c:url var="userCreateUrl"
                       value="/safety/user/UserCreate.action"/>

                <a href="${teacherHomeUrl}">ホーム</a>
                <a href="${userCreateUrl}">ユーザー登録</a>
            </c:when>

            <%-- 校長先生 --%>
            <c:when test="${sessionScope.user.role eq 'PRINCIPAL'}">
                <c:url var="principalHomeUrl"
                       value="/safety/principal/Home.action"/>

                <a href="${principalHomeUrl}">ホーム</a>
            </c:when>
        </c:choose>
    </nav>
</c:if>