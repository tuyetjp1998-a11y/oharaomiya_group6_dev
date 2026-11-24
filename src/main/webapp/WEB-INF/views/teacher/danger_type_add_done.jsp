<%-- 「困っている」の項目追加完了画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="追加完了" />
<%@ include file="parts/header.jspf" %>

<h1>追加しました</h1>
<section class="card danger-types-panel">
    <h2><c:out value="${addedName}" /></h2>
    <p>全学年・全クラスに反映しました。</p>
</section>
<a class="primary group-submit danger-type-return"
   href="${dangerTypesUrl}">一覧へ</a>

<%@ include file="parts/footer.jspf" %>
