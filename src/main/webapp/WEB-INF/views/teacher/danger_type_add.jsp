<%-- 「困っている」の項目追加画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="困っている内容を追加" />
<%@ include file="parts/header.jspf" %>

<a class="back" href="${dangerTypesUrl}">← 困っている内容</a>
<h1>困っている内容を追加</h1>

<form class="card form danger-types-panel"
      action="${dangerTypeAddUrl}" method="post">
    <input type="hidden" name="csrfToken"
           value="${sessionScope.csrfToken}">
    <label for="danger-type-name">項目名</label>
    <input id="danger-type-name" name="name" maxlength="30"
           value="<c:out value='${dangerTypeName}' />"
           placeholder="例：熱中症" autocomplete="off" required>
    <c:if test="${not empty addError}">
        <p role="alert"><c:out value="${addError}" /></p>
    </c:if>
    <div class="danger-type-actions">
        <button class="primary" type="submit">追加する</button>
        <a class="plain-button" href="${dangerTypeAddUrl}">キャンセル</a>
    </div>
</form>

<%@ include file="parts/footer.jspf" %>
