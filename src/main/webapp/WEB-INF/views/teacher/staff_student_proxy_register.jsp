<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="生徒の代理登録" />
<%@ include file="parts/header.jspf" %>

<h1>生徒の代理登録</h1>
<form class="card form" method="post" action="${studentProxyRegisterUrl}">
    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">

    <label for="name">氏名</label>
    <input id="name" name="name" value="<c:out value='${inputName}' />"
           required maxlength="40" autocomplete="name">

    <label for="login-id">ユーザーID</label>
    <input id="login-id" name="loginId" value="<c:out value='${inputLoginId}' />"
           required maxlength="40" autocomplete="off">

    <label for="password">初期パスワード</label>
    <input id="password" name="password" type="password"
           required maxlength="72" autocomplete="new-password">

    <label for="class-id">担当クラス</label>
    <select id="class-id" name="classId" required>
        <c:forEach var="room" items="${assignedClasses}">
            <option value="${room.classId}"
                    ${room.classId == selectedClassId ? 'selected' : ''}>
                <c:out value="${room.className}" />
            </option>
        </c:forEach>
    </select>

    <c:if test="${not empty proxyRegisterError}">
        <p role="alert"><c:out value="${proxyRegisterError}" /></p>
    </c:if>

    <button class="primary group-submit" type="submit"
            ${empty assignedClasses ? 'disabled' : ''}>
        生徒を代理登録
    </button>
</form>

<%@ include file="parts/footer.jspf" %>
