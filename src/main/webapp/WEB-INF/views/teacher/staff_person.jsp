<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="回答詳細" />
<%@ include file="parts/header.jspf" %>

<h1>回答詳細</h1>
<a class="back" href="${answersUrl}">← 回答一覧</a>
<div class="card">
  <div class="row">
    <span>氏名</span><b><c:out value="${detailStudent.name}" /></b>
  </div>
  <div class="row">
    <span>安否</span><b class="status-badge ${detailStudent.status == 'WORRIED' ? 'worried' : (detailStudent.status == 'DANGER' ? 'danger' : (detailStudent.status == 'SAFE' ? 'safe' : ''))}"><c:choose><c:when test="${detailReport.status == 'WORRIED' and not empty detailWorriedName}">困っている（<c:out value="${detailWorriedName}" />）</c:when><c:otherwise><c:out value="${detailStudent.statusLabel}" /></c:otherwise></c:choose></b>
  </div>
  <div class="row answer-comment-row">
    <span>コメント</span><p id="staff-answer-comment"><c:out value="${empty detailReport.comment ? 'なし' : detailReport.comment}" /></p>
  </div>
  <div class="row">
    <span>送信日時</span><span><c:out value="${detailStudent.displayTime}" /></span>
  </div>
  <div class="row">
    <span>送信位置</span><span><c:choose><c:when test="${detailReport.latitude != null and detailReport.longitude != null}"><c:out value="${detailReport.latitude}" />、<c:out value="${detailReport.longitude}" /></c:when><c:otherwise>未取得</c:otherwise></c:choose></span>
  </div>
</div>

<%@ include file="parts/footer.jspf" %>
