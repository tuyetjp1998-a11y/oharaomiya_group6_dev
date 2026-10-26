<%-- 生徒の安否詳細画面 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">

<title>お子さんの安否 | AnpiLink</title>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="a">
	<c:url var="homeUrl" value="/safety/family/Home.action" />
	<c:url var="moreUrl" value="/safety/family/More.action" />

	<div class="phone">
		<header class="phone-header">
			<div class="brand">AnpiLink</div>
		</header>

		<main class="page">
			<a class="back" href="${homeUrl}">← 戻る</a>

			<h1>お子さんの安否</h1>

			<h2>
				<c:out value="${requestScope.student.studentName}" />
				さん
			</h2>

			<c:choose>
				<c:when test="${empty requestScope.report}">
					<div class="card">
						<p class="muted">まだ安否回答がありません。</p>
					</div>
				</c:when>

				<c:otherwise>
					<section class="card">
						<div class="row">
							<span>状況</span>

							<c:choose>
								<c:when test="${report.status eq 'SAFE'}">
									<b class="status-badge safe">無事</b>
								</c:when>
								<c:otherwise>
									<b class="status-badge danger">危険</b>
								</c:otherwise>
							</c:choose>
						</div>

						<c:if test="${report.status eq 'DANGER'}">
							<div class="row">
								<span>危険の種類</span> <span> <c:choose>
										<c:when test="${report.dangerType eq 'ACCIDENT'}">
                                            事故
                                        </c:when>
										<c:when test="${report.dangerType eq 'EARTHQUAKE'}">
                                            地震
                                        </c:when>
										<c:when test="${report.dangerType eq 'FIRE'}">
                                            火災
                                        </c:when>
										<c:when test="${report.dangerType eq 'LANDSLIDE'}">
                                            土砂崩れ
                                        </c:when>
										<c:when test="${report.dangerType eq 'STALKING'}">
                                            付きまとわれている
                                        </c:when>
										<c:otherwise>
                                            不明
                                        </c:otherwise>
									</c:choose>
								</span>
							</div>
						</c:if>

						<div class="row">
							<span>回答日時</span> <span> <c:out
									value="${requestScope.reportedAtText}" />
							</span>
						</div>

						<div class="row">
							<span>位置情報</span>

							<c:choose>
								<c:when
									test="${report.latitude ne null
                                                and report.longitude ne null}">
									<span>取得済み</span>
								</c:when>
								<c:otherwise>
									<span class="muted">未取得</span>
								</c:otherwise>
							</c:choose>
						</div>

						<c:if
							test="${report.latitude ne null
                                      and report.longitude ne null}">
							<div class="row">
								<span>緯度</span> <span> <c:out value="${report.latitude}" />
								</span>
							</div>

							<div class="row">
								<span>経度</span> <span> <c:out value="${report.longitude}" />
								</span>
							</div>
							<%-- 安否回答時に保存された位置を表示する --%>
							<div id="childLocationMap"
								data-latitude="<c:out value='${report.latitude}' />"
								data-longitude="<c:out value='${report.longitude}' />">

								<iframe id="childLocationMapFrame" class="student-location-map"
									title="お子さんの安否回答時の位置" loading="lazy"
									referrerpolicy="no-referrer"> </iframe>

								<p class="muted">安否回答時に取得した位置です。現在地ではありません。</p>
							</div>
						</c:if>
					</section>
				</c:otherwise>
			</c:choose>
		</main>

		<footer class="app-footer mobile-footer"> ＠AnpiLink </footer>

		<nav class="phone-tabs tabs two" aria-label="メインメニュー">
			<a href="${homeUrl}"> <svg viewBox="0 0 24 24" aria-hidden="true"
					fill="none" stroke="currentColor" stroke-width="1.7"
					stroke-linecap="round" stroke-linejoin="round">
                    <path d="m3 10 9-7 9 7M5 9v12h14V9M9 21v-7h6v7" />
                </svg> <span>ホーム</span>
			</a> <a href="${moreUrl}"> <svg viewBox="0 0 24 24"
					aria-hidden="true" fill="none" stroke="currentColor"
					stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M5 6h14M5 12h14M5 18h14" />
                </svg> <span>その他</span>
			</a>
		</nav>
	</div>
	<script>
(function () {
    "use strict";

    var map = document.getElementById("childLocationMap");
    var frame = document.getElementById("childLocationMapFrame");

    // 位置情報がない場合は地図を表示しない
    if (!map || !frame) {
        return;
    }

    var latitude = Number(map.getAttribute("data-latitude"));
    var longitude = Number(map.getAttribute("data-longitude"));

    // 座標の範囲を確認する
    if (!Number.isFinite(latitude)
            || !Number.isFinite(longitude)
            || latitude < -90 || latitude > 90
            || longitude < -180 || longitude > 180) {
        map.hidden = true;
        return;
    }

    // 保存された座標を中心に表示範囲を設定する
    var west = Math.max(-180, longitude - 0.005);
    var east = Math.min(180, longitude + 0.005);
    var south = Math.max(-90, latitude - 0.005);
    var north = Math.min(90, latitude + 0.005);

    var params = new URLSearchParams();

    params.set("bbox", [west, south, east, north].join(","));
    params.set("layer", "mapnik");
    params.set("marker", latitude + "," + longitude);

    frame.src =
        "https://www.openstreetmap.org/export/embed.html?"
        + params.toString();
})();
</script>
</body>
</html>