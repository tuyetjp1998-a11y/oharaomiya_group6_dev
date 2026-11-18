(function () {
    "use strict";

    var script = document.currentScript;
    var introUrl = script.getAttribute("data-intro-url");
    var statusUrl = script.getAttribute("data-status-url");
    var scope = script.getAttribute("data-worker-scope");

    function openIntro() {
        window.location.replace(introUrl);
    }

    if (!window.isSecureContext
            || !("Notification" in window)
            || !("serviceWorker" in navigator)
            || !("PushManager" in window)
            || Notification.permission !== "granted") {
        openIntro();
        return;
    }

    navigator.serviceWorker.getRegistration(scope)
        .then(function (registration) {
            if (!registration) {
                openIntro();
                return;
            }

            return registration.pushManager.getSubscription()
                .then(function (subscription) {
                    if (!subscription) {
                        openIntro();
                        return;
                    }

                    return fetch(statusUrl, {
                        credentials: "same-origin",
                        cache: "no-store",
                        redirect: "error",
                        headers: {
                            "X-Push-Endpoint": subscription.endpoint
                        }
                    }).then(function (response) {
                        if (response.status === 403) {
                            openIntro();
                        }
                        // 204 は登録済み。通信エラー時もホームを利用できる。
                    });
                });
        })
        .catch(function () {
            // 通知の確認ができなくても、安否機能は利用できる。
        });
})();