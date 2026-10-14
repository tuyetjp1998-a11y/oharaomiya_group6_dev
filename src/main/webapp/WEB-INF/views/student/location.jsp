<%-- 生徒の位置情報画面 --%>
<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport"
          content="width=device-width, initial-scale=1">

    <title>位置情報 | AnpiLink</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/style.css?v=20261014-1">
</head>
<body class="a">
    <%-- 遷移先URL --%>
    <c:url var="homeUrl"
           value="/safety/student/Home.action" />
    <c:url var="moreUrl"
           value="/safety/student/More.action" />

    <div class="phone">
        <header class="phone-header">
            <div class="brand">AnpiLink</div>
        </header>

        <main class="page">
    <h1>位置情報</h1>

    <div class="card">
        <div class="map-placeholder">
            地図表示は準備中です。
        </div>

        <p id="locationStatus" role="status" aria-live="polite">
            未取得
        </p>

        <div id="locationDetails" hidden>
            <div class="row">
                <span>緯度</span>
                <span id="latitudeText"></span>
            </div>

            <div class="row">
                <span>経度</span>
                <span id="longitudeText"></span>
            </div>

            <div class="row">
                <span>位置精度</span>
                <span id="accuracyText"></span>
            </div>

            <div class="row">
                <span>取得日時</span>
                <span id="locationTimeText"></span>
            </div>
        </div>

        <p class="muted">
            取得した位置情報は、この画面にのみ表示します。
        </p>
    </div>

    <button type="button"
            class="primary group-submit"
            id="getLocationButton">
        現在地を取得
    </button>
</main>

        <footer class="app-footer mobile-footer">
            ＠AnpiLink
        </footer>

        <nav class="phone-tabs tabs two"
             aria-label="メインメニュー">
            <a href="${homeUrl}">
                <svg viewBox="0 0 24 24"
                     aria-hidden="true"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="1.7"
                     stroke-linecap="round"
                     stroke-linejoin="round">
                    <path d="m3 10 9-7 9 7M5 9v12h14V9M9 21v-7h6v7" />
                </svg>

                <span>ホーム</span>
            </a>

            <a href="${moreUrl}">
                <svg viewBox="0 0 24 24"
                     aria-hidden="true"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="1.7"
                     stroke-linecap="round"
                     stroke-linejoin="round">
                    <path d="M5 6h14M5 12h14M5 18h14" />
                </svg>

                <span>その他</span>
            </a>
        </nav>
    </div>
    
    <script>
    const getLocationButton =
            document.getElementById("getLocationButton");
    const locationStatus =
            document.getElementById("locationStatus");
    const locationDetails =
            document.getElementById("locationDetails");

    getLocationButton.addEventListener("click", function () {
        // 前回の表示をクリア
        locationDetails.hidden = true;

        if (!window.isSecureContext) {
            locationStatus.textContent =
                    "HTTPSでアクセスしてください。";
            return;
        }

        if (!navigator.geolocation) {
            locationStatus.textContent =
                    "このブラウザーは位置情報の取得に対応していません。";
            return;
        }

        getLocationButton.disabled = true;
        locationStatus.textContent = "位置情報を取得しています…";

        navigator.geolocation.getCurrentPosition(
            function (position) {
                const coords = position.coords;

                document.getElementById("latitudeText").textContent =
                        coords.latitude.toFixed(6);

                document.getElementById("longitudeText").textContent =
                        coords.longitude.toFixed(6);

                document.getElementById("accuracyText").textContent =
                        "約 " + Math.round(coords.accuracy) + " m";

                document.getElementById("locationTimeText").textContent =
                        new Date(position.timestamp).toLocaleString(
                            "ja-JP",
                            { timeZone: "Asia/Tokyo" }
                        );

                locationDetails.hidden = false;
                locationStatus.textContent = "取得済み";
                getLocationButton.disabled = false;
            },

            function (error) {
                switch (error.code) {
                    case 1:
                        locationStatus.textContent =
                                "位置情報の利用が許可されていません。"
                                + "ブラウザーや端末の設定を確認してください。";
                        break;

                    case 2:
                        locationStatus.textContent =
                                "位置情報を取得できませんでした。"
                                + "端末の位置情報設定を確認してください。";
                        break;

                    case 3:
                        locationStatus.textContent =
                                "取得に時間がかかっています。"
                                + "もう一度お試しください。";
                        break;

                    default:
                        locationStatus.textContent =
                                "位置情報の取得に失敗しました。";
                }

                getLocationButton.disabled = false;
            },

            {
                enableHighAccuracy: true,
                timeout: 15000,
                maximumAge: 0
            }
        );
    });
</script>
</body>
</html>