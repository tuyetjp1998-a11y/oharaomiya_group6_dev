/* この端末が指定された通知を受信できるか確認する */
function canReceiveNotification(actionPath) {
    var statusUrl = new URL(
        actionPath,
        self.registration.scope
    ).href;

    return self.registration.pushManager.getSubscription()
    .then(function (subscription) {
        if (!subscription) {
            return false;
        }

        return fetch(statusUrl, {
            method: "GET",
            credentials: "same-origin",
            cache: "no-store",
            redirect: "error",
            headers: {
                "X-Push-Endpoint": subscription.endpoint
            }
        })
        .then(function (response) {
            return response.status === 204;
        });
    })
    .catch(function () {
        // 確認できない場合は通知を表示しない
        return false;
    });
}

/* 通知の種類に応じて受信処理を行う */
self.addEventListener("push", function (event) {
    var payload = {};

    if (event.data) {
        try {
            payload = event.data.json();
        } catch (error) {
            // 不正な通知データは処理しない
            return;
        }
    }

    if (!payload
            || typeof payload !== "object"
            || Array.isArray(payload)) {
        return;
    }

    var type;
    var statusPath;
    var body;

    if (payload.type === "LOGIN_REMINDER") {
        type = "LOGIN_REMINDER";

        statusPath =
            "safety/auth/LoginReminderStatus.action";

        body =
            "ログインの有効期限が近づいています。" +
            "緊急時に備えて、再ログインしてください。";

    } else if (typeof payload.type === "undefined") {
        // 既存の安否通知は空のJSONで送信される
        type = "SAFETY_ANSWER";

        statusPath =
            "safety/family/PushStatus.action";

        body =
            "お子さんから安否回答が届きました。";

    } else {
        // 未対応の通知は処理しない
        return;
    }

    event.waitUntil(
        canReceiveNotification(statusPath)
        .then(function (allowed) {
            if (!allowed) {
                return;
            }

            // 個人情報やログイントークンは表示しない
            var options = {
                body: body,
                data: {
                    type: type
                }
            };

            if (type === "LOGIN_REMINDER") {
                options.tag = "anpilink-login-reminder";
            }

            return self.registration.showNotification(
                "AnpiLink",
                options
            );
        })
    );
});

/* 通知を押したときに対応する画面を開く */
self.addEventListener("notificationclick", function (event) {
    var data = event.notification.data || {};
    var targetPath;

    event.notification.close();

    if (data.type === "LOGIN_REMINDER") {
        // 自動ログインを行わず、再ログイン画面を表示する
        targetPath =
            "safety/auth/Login.action?reauth=true";

    } else {
        // 更新前に表示された安否通知にも対応する
        targetPath =
            "safety/family/Home.action";
    }

    var targetUrl = new URL(
        targetPath,
        self.registration.scope
    ).href;

    event.waitUntil(
        self.clients.openWindow(targetUrl)
    );
});