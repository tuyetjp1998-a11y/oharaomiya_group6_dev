/* 保護者向け通知設定 */
(function () {
    "use strict";

    var script = document.currentScript;
    var contextPath = script.getAttribute("data-context-path");
    var publicKey = script.getAttribute("data-vapid-public-key");

    var toggle = document.getElementById(
        "parent-notification-toggle"
    );
    var message = document.getElementById(
        "notification-message"
    );

    if (!toggle) {
        return;
    }

    var updateUrl = toggle.getAttribute("data-update-url");
    var csrfToken = toggle.getAttribute("data-csrf-token");
    var subscriptionUrl =
        contextPath + "/safety/family/PushSubscription.action";

    var stateText = toggle.querySelector(
        "[data-notification-state]"
    );

    var registration = null;
    var applicationServerKey = null;
    var updating = false;

    // メッセージを表示する
    function showMessage(text) {
        if (message) {
            message.textContent = text;
        }
    }

    // 保存済みの通知設定を表示する
    function updateDisplay(enabled) {
        toggle.setAttribute("aria-checked", String(enabled));

        if (stateText) {
            stateText.textContent = enabled ? "ON" : "OFF";
        }
    }

    // 公開鍵をバイト配列に変換する
    function convertPublicKey(value) {
        var base64 = value.replace(/-/g, "+")
            .replace(/_/g, "/");

        while (base64.length % 4 !== 0) {
            base64 += "=";
        }

        var raw = window.atob(base64);
        var bytes = new Uint8Array(raw.length);

        for (var i = 0; i < raw.length; i++) {
            bytes[i] = raw.charCodeAt(i);
        }

        if (bytes.length !== 65 || bytes[0] !== 4) {
            throw new Error("公開鍵の形式が正しくありません。");
        }

        return bytes;
    }

    // POSTを送信する（204には本文がない）
    function post(url, params) {
        return fetch(url, {
            method: "POST",
            credentials: "same-origin",
            redirect: "error",
            headers: {
                "Content-Type":
                    "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body: params.toString()
        })
        .then(function (response) {
            if (response.status === 401) {
                throw new Error(
                    "ログイン画面からログインし直してください。"
                );
            }

            if (response.status === 403) {
                throw new Error(
                    "画面を再読み込みして、もう一度お試しください。"
                );
            }

            if (response.status === 409) {
                throw new Error(
                    "このブラウザーの通知登録を保存できませんでした。" +
                    "別のアカウントで登録されていないか確認してください。"
                );
            }

            if (response.status !== 204) {
                throw new Error(
                    "設定を保存できませんでした。（HTTP " +
                    response.status + "）"
                );
            }
        });
    }

    // 通知を無効にする
    function disableNotifications() {
        var params = new URLSearchParams();

        params.set("enabled", "false");
        params.set("csrfToken", csrfToken);

        return post(updateUrl, params);
    }

    // 通知の利用許可を確認し、登録情報を保存する
    function enableNotifications() {
        if (!registration || !applicationServerKey) {
            return Promise.reject(new Error(
                "通知の準備が完了していません。"
            ));
        }

        // ユーザーのクリックを起点に許可を求める
        return Notification.requestPermission()
        .then(function (permission) {
            if (permission !== "granted") {
                throw new Error(
                    "通知が許可されていません。" +
                    "ブラウザーのサイト設定を確認してください。"
                );
            }

            return registration.pushManager.getSubscription();
        })
        .then(function (subscription) {
            if (subscription) {
                return subscription;
            }

            return registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: applicationServerKey
            });
        })
        .then(function (subscription) {
            var data = subscription.toJSON();

            if (!data.keys
                    || !data.keys.p256dh
                    || !data.keys.auth) {
                throw new Error(
                    "通知の登録情報を取得できませんでした。"
                );
            }

            var params = new URLSearchParams();

            params.set("csrfToken", csrfToken);
            params.set("endpoint", data.endpoint);
            params.set("p256dh", data.keys.p256dh);
            params.set("auth", data.keys.auth);

            return post(subscriptionUrl, params);
        });
    }

    // 必須設定を確認する
    if (!updateUrl || !csrfToken) {
        toggle.disabled = true;
        showMessage("通知設定を利用できません。");
        return;
    }

    // 保存中の連続操作を防止する
    toggle.addEventListener("click", function () {
        if (updating) {
            return;
        }

        var nextEnabled =
            toggle.getAttribute("aria-checked") !== "true";

        updating = true;
        toggle.disabled = true;
        showMessage("設定を保存しています...");

        var operation = nextEnabled
            ? enableNotifications()
            : disableNotifications();

        operation
        .then(function () {
            updateDisplay(nextEnabled);

            showMessage(nextEnabled
                ? "通知をONにしました。配信機能は準備中です。"
                : "通知をOFFにしました。");
        })
        .catch(function (error) {
            // endpointや認証情報はログに出力しない
            showMessage(
                error.message || "設定を保存できませんでした。"
            );
        })
        .then(function () {
            updating = false;
            toggle.disabled = false;
        });
    });

    // OFFの操作はWeb Push非対応の環境でも利用できる
    toggle.disabled = false;

    if (!window.isSecureContext
            || !("serviceWorker" in navigator)
            || !("PushManager" in window)
            || !("Notification" in window)) {
        showMessage(
            "この環境ではプッシュ通知を利用できません。"
        );
        return;
    }

    if (!publicKey) {
        showMessage("通知用の公開鍵が設定されていません。");
        return;
    }

    try {
        applicationServerKey = convertPublicKey(publicKey);
    } catch (error) {
        showMessage("通知用の公開鍵を確認してください。");
        return;
    }

    // Service Workerの有効化を待つ
    navigator.serviceWorker.register(
        contextPath + "/sw.js",
        { scope: contextPath + "/" }
    )
    .then(function () {
        return navigator.serviceWorker.ready;
    })
    .then(function (readyRegistration) {
        registration = readyRegistration;
    })
    .catch(function () {
        showMessage(
            "通知の準備に失敗しました。再読み込みしてください。"
        );
    });
})();