<%-- 「困っている」の登録済み項目一覧 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="困っている内容" />
<%@ include file="parts/header.jspf" %>

<div class="portal-heading">
    <div>
        <h1>困っている内容</h1>
        <p class="danger-types-scope">全学年・全クラス共通</p>
    </div>
    <a class="plain-button" href="${dangerTypeAddUrl}">追加する</a>
</div>

<c:if test="${not empty dangerTypeMessage}">
    <p role="status"><c:out value="${dangerTypeMessage}" /></p>
</c:if>

<section class="card danger-types-panel">
    <h2>登録済みの項目</h2>
    <c:choose>
        <c:when test="${empty worriedTypes}">
            <p class="muted">登録済みの項目はありません。</p>
        </c:when>
        <c:otherwise>
            <ul class="danger-types-list">
                <c:forEach var="item" items="${worriedTypes}">
                    <li>
                        <span><c:out value="${item.name}" /></span>
                        <a class="danger-type-delete"
                           href="#delete-danger-${item.id}"
                           aria-label="${fn:escapeXml(item.name)}を削除">
                            削除
                        </a>
                    </li>
                </c:forEach>
            </ul>
        </c:otherwise>
    </c:choose>
</section>

<c:forEach var="item" items="${worriedTypes}">
    <div class="modal"
         id="delete-danger-${item.id}"
         role="dialog"
         aria-modal="true"
         aria-labelledby="delete-danger-${item.id}-title">
        <div class="dialog">
            <h2 id="delete-danger-${item.id}-title">
                「<c:out value="${item.name}" />」を削除しますか？
            </h2>
            <div>
                <a class="plain-button" href="${dangerTypesUrl}">
                    キャンセル
                </a>
                <form action="${dangerTypeDeleteUrl}" method="post">
                    <input type="hidden" name="csrfToken"
                           value="${sessionScope.csrfToken}">
                    <input type="hidden" name="id" value="${item.id}">
                    <button class="delete-button" type="submit">
                        削除する
                    </button>
                </form>
            </div>
        </div>
    </div>
</c:forEach>

<%@ include file="parts/footer.jspf" %>
