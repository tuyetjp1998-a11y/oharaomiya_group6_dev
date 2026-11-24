<%-- 親子関係申請の却下完了画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="申請の却下完了" />
<%@ include file="parts/header.jspf" %>

<h1>申請を却下しました</h1>
<section class="card">
    <span class="status-badge request-rejected">却下済み</span>
    <p>却下理由を保護者の申請履歴に表示します。</p>
</section>
<a class="primary group-submit" href="${familyRequestsUrl}">申請一覧へ</a>

<%@ include file="parts/footer.jspf" %>
