<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="完了" />
<%@ include file="parts/header.jspf" %>

<h1>完了</h1>
<div class="card">操作が完了しました。</div>
<a class="primary group-submit" href="${groupsUrl}">戻る</a>

<%@ include file="parts/footer.jspf" %>
