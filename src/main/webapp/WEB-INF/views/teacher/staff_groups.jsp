<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="グループ" />
<%@ include file="parts/header.jspf" %>

<h1>グループ</h1>
<div class="actions">
    <a href="${studentAddUrl}">登録済みの生徒を追加</a>
    <a href="${studentProxyRegisterUrl}">生徒を代理登録</a>
</div>
<c:forEach var="room" items="${assignedClasses}">
    <h2><c:out value="${room.className}" /></h2>
    <div class="table">
        <c:forEach var="student" items="${room.students}">
            <div class="row">
                <span><c:out value="${student.name}" /></span>
                <span><c:out value="${student.loginId}" /></span>
                <div>
                    <form method="post" action="${studentRemoveUrl}">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="classId" value="${room.classId}">
                        <input type="hidden" name="studentId" value="${student.studentId}">
                        <button type="submit" class="plain-button">クラスから外す</button>
                    </form>
                </div>
            </div>
        </c:forEach>
    </div>
</c:forEach>

<%@ include file="parts/footer.jspf" %>
