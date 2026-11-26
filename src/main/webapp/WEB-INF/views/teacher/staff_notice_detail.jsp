<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="screenTitle" value="連絡詳細" />
<%@ include file="parts/header.jspf" %>
<h1>連絡詳細</h1>
<c:url var="noticeListUrl" value="/safety/teacher/Notices.action" />
<a class="back" href="${noticeListUrl}">← 連絡一覧</a>
<div class="card">
    <h2 style="overflow-wrap:anywhere;"><c:out value="${notice.title}" /></h2>
    <p>配信元：校長</p>
    <p>配信日時：<c:out value="${notice.displayTime}" /></p>
    <div class="row" style="display:block; white-space:pre-wrap; overflow-wrap:anywhere;"><c:out value="${notice.message}" /></div>
</div>

<%@ include file="parts/footer.jspf" %>
