"use strict";

document.querySelectorAll(".answer-location-form")
    .forEach(function (form) {
        let collecting = false;
        let readyToSubmit = false;

        const message =
                form.querySelector(".location-message");

        const fieldNames = [
            "latitude",
            "longitude",
            "locationAccuracy",
            "locationAcquiredAt"
        ];

        form.addEventListener("submit", function (event) {
            if (readyToSubmit) {
                return;
            }

            event.preventDefault();

            if (collecting) {
                return;
            }

            const submitter = event.submitter;

            collecting = true;

            // 前回の位置情報をクリア
            fieldNames.forEach(function (name) {
                form.elements.namedItem(name).value = "";
            });

            const buttons =
                    Array.from(form.querySelectorAll(
                        'button[type="submit"]'));

            buttons.forEach(function (button) {
                button.disabled = true;
            });

            message.textContent =
                    "位置情報を取得しています…";

            function sendAnswer() {
                readyToSubmit = true;

                buttons.forEach(function (button) {
                    button.disabled = false;
                });

                if (submitter) {
                    form.requestSubmit(submitter);
                } else {
                    form.requestSubmit();
                }
            }

            function sendWithoutLocation() {
                message.textContent =
                        "位置情報を取得できなかったため、"
                        + "安否のみ送信します。";

                sendAnswer();
            }

            // OFFの場合は位置情報を取得せずに送信
			if (form.dataset.locationEnabled !== "true") {
			    message.textContent = "安否を送信しています…";
			    sendAnswer();
			    return;
			}
			
			if (!window.isSecureContext
			        || !navigator.geolocation) {
			    sendWithoutLocation();
			    return;
			}

            navigator.geolocation.getCurrentPosition(
                function (position) {
                    const coords = position.coords;

                    form.elements.namedItem("latitude").value =
                            String(coords.latitude);

                    form.elements.namedItem("longitude").value =
                            String(coords.longitude);

                    form.elements.namedItem(
                            "locationAccuracy").value =
                            String(coords.accuracy);

                    form.elements.namedItem(
                            "locationAcquiredAt").value =
                            new Date(position.timestamp)
                                .toISOString();

                    message.textContent = "送信しています…";

                    sendAnswer();
                },

                function () {
                    sendWithoutLocation();
                },

                {
                    enableHighAccuracy: true,
                    timeout: 10000,
                    maximumAge: 0
                }
            );
        });
    });