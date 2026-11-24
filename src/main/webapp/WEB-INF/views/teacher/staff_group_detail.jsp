<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="クラス詳細" />
<%@ include file="parts/header.jspf" %>

<h1>クラス詳細</h1>
<a class="back" href="${groupsUrl}">← グループ</a>
<h2><c:out value="${detailClass.className}" /></h2>
<div class="table">
    <c:forEach var="student" items="${detailClass.students}">
        <div class="row">
            <span><c:out value="${student.name}" /></span><span><c:out value="${student.loginId}" /></span>
        </div>
    </c:forEach>
</div>

<%@ include file="parts/footer.jspf" %>
