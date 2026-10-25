/* 現在の利用者が通知を受信できるか確認する */
function canReceivePush() {
    var statusUrl = new URL(
        "safety/family/PushStatus.action",
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
        // 確認できない場合は安否通知を表示しない
        return false;
    });
}

/* 安否回答のプッシュ通知を受信する */
self.addEventListener("push", function (event) {
    var homeUrl = new URL(
        "safety/family/Home.action",
        self.registration.scope
    ).href;

    event.waitUntil(
        canReceivePush()
        .then(function (allowed) {
            if (!allowed) {
                return;
            }

            // 詳しい安否情報はログイン後の画面で確認する
            return self.registration.showNotification(
                "AnpiLink",
                {
                    body: "お子さんから安否回答が届きました。",
                    data: {
                        url: homeUrl
                    }
                }
            );
        })
    );
});

/* 通知を押したときに保護者ホームを開く */
self.addEventListener("notificationclick", function (event) {
    event.notification.close();

    var homeUrl = new URL(
        "safety/family/Home.action",
        self.registration.scope
    ).href;

    event.waitUntil(
        clients.openWindow(homeUrl)
    );
});