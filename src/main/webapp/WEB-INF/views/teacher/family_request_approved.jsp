<%-- 親子関係申請の承認完了画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="申請の承認完了" />
<%@ include file="parts/header.jspf" %>

<h1>申請を承認しました</h1>
<section class="card">
    <span class="status-badge safe">承認済み</span>
    <p>保護者と生徒の関連付けを登録しました。</p>
</section>
<a class="primary group-submit" href="${familyRequestsUrl}">申請一覧へ</a>

<%@ include file="parts/footer.jspf" %>
