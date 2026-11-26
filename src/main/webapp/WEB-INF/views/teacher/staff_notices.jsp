<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="screenTitle" value="学校からの連絡" />
<%@ include file="parts/header.jspf" %>
<h1>学校からの連絡</h1>
<c:if test="${empty notices}">
    <div class="card"><p class="muted">学校からの連絡はありません。</p></div>
</c:if>
<c:forEach var="notice" items="${notices}">
    <c:url var="noticeDetailUrl" value="/safety/teacher/NoticeDetail.action" />
    <div class="card">
        <form method="post" action="${noticeDetailUrl}" style="margin:0;">
            <input type="hidden" name="csrfToken" value="<c:out value="${sessionScope.csrfToken}" />" />
            <input type="hidden" name="dispatchId" value="<c:out value="${notice.dispatchId}" />" />
            <button type="submit" class="row" style="display:flex; width:100%; justify-content:space-between; align-items:center; text-align:left; text-decoration:none; color:inherit; background:transparent; border:0; padding:0; font:inherit; cursor:pointer;">
                <span style="min-width:0; overflow-wrap:anywhere;">
                    <c:if test="${notice.unread}">
                        <span aria-label="未読" title="未読" style="display:inline-block; width:9px; height:9px; border-radius:50%; background:#2866C9; margin-right:8px; vertical-align:middle;"></span>
                    </c:if>
                    <strong><c:out value="${notice.title}" /></strong>
                    <span class="muted" style="display:block; margin-top: 8px;"><c:out value="${notice.displayTime}" /></span>
                </span>
                <span aria-hidden="true">›</span>
            </button>
        </form>
    </div>
</c:forEach>

<%@ include file="parts/footer.jspf" %>
