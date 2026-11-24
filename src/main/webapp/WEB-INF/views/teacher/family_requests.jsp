<%-- 保護者の申請一覧画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="保護者の申請" />
<%@ include file="parts/header.jspf" %>

<h1>保護者の申請</h1>
<p class="request-help">担当クラスの生徒への申請</p>

<c:choose>
    <c:when test="${empty requests}">
        <section class="card request-list">
            <p class="request-help">申請はありません。</p>
        </section>
    </c:when>
    <c:otherwise>
        <c:forEach var="item" items="${requests}">
            <section class="card request-list">
                <div class="request-heading">
                    <h2><c:out value="${item.familyName}" /></h2>
                    <c:choose>
                        <c:when test="${item.status eq 'PENDING'}">
                            <span class="status-badge request-pending">承認待ち</span>
                        </c:when>
                        <c:when test="${item.status eq 'APPROVED'}">
                            <span class="status-badge safe">承認済み</span>
                        </c:when>
                        <c:otherwise>
                            <span class="status-badge request-rejected">却下済み</span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <p>
                    申請先：<c:out value="${item.studentName}" />（<c:out value="${item.studentLoginId}" />）／<c:out value="${item.className}" />
                </p>
                <c:if test="${item.status eq 'PENDING'}">
                    <c:url var="detailUrl" value="/safety/teacher/FamilyRequestDetail.action">
                        <c:param name="requestId" value="${item.id}" />
                    </c:url>
                    <a class="plain-button" href="${detailUrl}">申請を確認 →</a>
                </c:if>
            </section>
        </c:forEach>
    </c:otherwise>
</c:choose>

<%@ include file="parts/footer.jspf" %>
