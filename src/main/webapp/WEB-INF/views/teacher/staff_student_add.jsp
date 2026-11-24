<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="自クラスに生徒を追加" />
<%@ include file="parts/header.jspf" %>

<h1>自クラスに生徒を追加</h1>
<a class="back" href="${groupsUrl}">← グループ</a>
<form class="card form" method="post" action="${studentAddUrl}">
    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
    <label for="class-id">担当クラス</label>
    <select id="class-id" name="classId" required>
        <c:forEach var="room" items="${assignedClasses}">
            <option value="${room.classId}" ${room.classId == selectedClassId ? 'selected' : ''}><c:out value="${room.className}" /></option>
        </c:forEach>
    </select>
    <label for="student-id">登録済みの生徒ID</label>
    <input id="student-id" name="studentLoginId" value="<c:out value='${studentLoginId}' />" required>
    <c:if test="${not empty addError}"><p role="alert"><c:out value="${addError}" /></p></c:if>
    <button class="primary group-submit" type="submit" ${empty assignedClasses ? 'disabled' : ''}>追加する</button>
</form>

<%@ include file="parts/footer.jspf" %>
