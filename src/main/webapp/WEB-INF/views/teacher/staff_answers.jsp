<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="担当クラスの安否" />
<%@ include file="parts/header.jspf" %>

<h1>担当クラスの安否</h1>
<div class="table staff-answer-table">
    <c:forEach var="row" items="${tableRows}">
        <c:url var="detailUrl" value="/safety/teacher/Home.action">
            <c:param name="view" value="answers" />
            <c:param name="studentId" value="${row.studentId}" />
        </c:url>
        <a class="row" href="${detailUrl}">
            <span><c:out value="${row.name}" /></span>
            <span><c:out value="${row.className}" /></span>
            <b class="status-badge ${row.status == 'SAFE' ? 'safe' : (row.status == 'DANGER' ? 'danger' : (row.status == 'WORRIED' ? 'worried' : ''))}"><c:out value="${row.statusLabel}" /></b>
            <span aria-hidden="true">›</span>
        </a>
    </c:forEach>
</div>

<%@ include file="parts/footer.jspf" %>
