<%-- 親子関係の申請確認画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="親子関係の確認" />
<%@ include file="parts/header.jspf" %>

<a class="back" href="${familyRequestsUrl}">← 申請一覧へ</a>
<h1>親子関係の確認</h1>

<section class="card">
    <div class="request-heading">
        <h2>申請内容</h2>
        <span class="status-badge request-pending">承認待ち</span>
    </div>
    <dl class="request-details">
        <dt>保護者</dt>
        <dd><c:out value="${familyRequest.familyName}" />（<c:out value="${familyRequest.familyLoginId}" />）</dd>
        <dt>生徒</dt>
        <dd><c:out value="${familyRequest.studentName}" />（<c:out value="${familyRequest.studentLoginId}" />）</dd>
        <dt>クラス</dt>
        <dd><c:out value="${familyRequest.className}" /></dd>
    </dl>
</section>

<form class="card form" action="${familyRequestReviewUrl}" method="post">
    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
    <input type="hidden" name="requestId" value="${familyRequest.id}">
    <input type="hidden" name="decision" value="APPROVED">
    <p>学校の登録情報・保護者への連絡で確認してください。</p>
    <label class="group-check" for="relationship-confirmed">
        <input id="relationship-confirmed" type="checkbox"
               name="relationshipConfirmed" value="confirmed" required>
        親子関係を確認しました
    </label>
    <div class="request-actions">
        <button class="primary group-submit" type="submit">承認する</button>
        <c:url var="rejectUrl" value="/safety/teacher/FamilyRequestDetail.action">
            <c:param name="requestId" value="${familyRequest.id}" />
            <c:param name="mode" value="reject" />
        </c:url>
        <a class="plain-button" href="${rejectUrl}">却下する</a>
    </div>
</form>

<%@ include file="parts/footer.jspf" %>
