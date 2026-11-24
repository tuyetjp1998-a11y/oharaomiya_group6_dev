<%-- 親子関係申請の却下画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="申請の却下" />
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
    <input type="hidden" name="decision" value="REJECTED">
    <label for="rejection-reason">却下理由</label>
    <textarea id="rejection-reason" name="rejectionReason"
              rows="4" maxlength="300" required>親子関係を確認できませんでした。学校へお問い合わせください。</textarea>
    <p class="request-help">却下理由は保護者に表示されます。</p>
    <div class="request-actions">
        <button class="delete-button" type="submit">却下を確定する</button>
        <c:url var="detailUrl" value="/safety/teacher/FamilyRequestDetail.action">
            <c:param name="requestId" value="${familyRequest.id}" />
        </c:url>
        <a class="plain-button" href="${detailUrl}">キャンセル</a>
    </div>
</form>

<%@ include file="parts/footer.jspf" %>
