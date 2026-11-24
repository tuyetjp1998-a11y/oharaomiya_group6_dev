<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="screenTitle" value="職員ホーム" />
<%@ include file="parts/header.jspf" %>

<div class="portal-heading">
    <div>
        <p class="eyebrow">職員ホーム</p>
        <h1>担当クラスの安否状況</h1>
    </div>

    <div class="portal-date">
        <c:out value="${todayLabel}" />
        <span>最終回答：<c:out value="${empty latestReports ? 'なし' : latestReports[0].displayTime}" /></span>
    </div>
</div>

<section class="stats">
    <div class="stat">
        <span>回答済み</span>
        <div>
            <strong>${answered}</strong><small> / ${studentTotal}人</small>
        </div>
        <div class="progress" aria-hidden="true"><i style="width:${answerPercent}%"></i></div>
    </div>

    <div class="stat">
        <span>未回答</span>

        <div>
            <strong>${unanswered}</strong>
            <small>人</small>
        </div>

        <p>${studentTotal == 0 ? '対象の生徒はいません。' : (unanswered == 0 ? '全員回答済みです' : '回答を待っています。')}</p>
    </div>

    <div class="stat danger-stat">
        <span>要対応</span>

        <div>
            <strong>${attention}</strong>
            <small>人</small>
        </div>

        <p>${attention > 0 ? '確認が必要です' : '要対応の生徒はいません。'}</p>
    </div>
</section>

<div class="portal-content">
    <section class="alarm-panel">
        <div class="section-heading">
            <h2>
                <svg viewBox="0 0 24 24"
                     aria-hidden="true"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="1.7"
                     stroke-linecap="round"
                     stroke-linejoin="round">
                    <path d="m12 3 10 18H2ZM12 9v5m0 3v.1" />
                </svg>
                危険・困っている生徒
            </h2>

            <span class="count">${attention}人</span>
        </div>

        <div class="danger-list">
            <c:if test="${attention == 0}">
                <p class="muted">要対応の生徒はいません。</p>
            </c:if>
            <c:forEach var="room" items="${assignedClasses}">
                <c:forEach var="student" items="${room.attentionStudents}">
                    <c:url var="attentionDetailUrl" value="/safety/teacher/Home.action">
                        <c:param name="view" value="answers" />
                        <c:param name="studentId" value="${student.studentId}" />
                    </c:url>
                    <div class="danger-person">
                        <span class="avatar" aria-hidden="true"><c:out value="${fn:substring(student.name, 0, 1)}" /></span>
                        <div>
                            <strong><c:out value="${student.name}" /></strong>
                            <span class="person-class"><c:out value="${room.className}" /></span>
                        </div>
                        <span class="badge ${student.status == 'DANGER' ? 'danger' : 'worried'}"><c:out value="${student.statusLabel}" /></span>
                        <a class="detail-button" href="${attentionDetailUrl}">
                            確認
                            <svg viewBox="0 0 24 24"
                                 aria-hidden="true"
                                 fill="none"
                                 stroke="currentColor"
                                 stroke-width="1.7"
                                 stroke-linecap="round"
                                 stroke-linejoin="round">
                                <path d="M5 12h14m-5-5 5 5-5 5" />
                            </svg>
                        </a>
                    </div>
                </c:forEach>
            </c:forEach>
        </div>
    </section>

    <section class="answers-panel">
        <div class="section-heading">
            <h2>最新の安否回答</h2>
            <a class="text-button" href="${answersUrl}">一覧を見る</a>
        </div>

        <div class="table-scroll">
            <table>
                <thead>
                    <tr>
                        <th scope="col">生徒</th>
                        <th scope="col">クラス</th>
                        <th scope="col">安否</th>
                        <th scope="col">回答時刻</th>

                    </tr>
                </thead>

                <tbody>
                    <c:if test="${empty tableRows}">
                        <tr><td colspan="4" class="muted">安否回答はまだありません。</td></tr>
                    </c:if>
                    <c:forEach var="row" items="${tableRows}">
                        <tr>
                            <td><span class="table-name"><c:out value="${row.name}" /></span></td>
                            <td><c:out value="${row.className}" /></td>
                            <td><span class="badge ${row.status == 'SAFE' ? 'safe' : (row.status == 'DANGER' ? 'danger' : (row.status == 'WORRIED' ? 'worried' : ''))}"><c:out value="${row.statusLabel}" /></span></td>
                            <td><c:out value="${row.displayTime}" /></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </section>
</div>

<div class="portal-foot">
    担当クラス：<c:if test="${empty assignedClasses}">なし</c:if><c:forEach
        var="room" items="${assignedClasses}" varStatus="loop"><c:if
        test="${not loop.first}">、</c:if><c:out value="${room.className}" /></c:forEach>
</div>

<%@ include file="parts/footer.jspf" %>
