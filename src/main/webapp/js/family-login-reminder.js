/* 保護者のログイン期限通知を登録する */
(function() {
	"use strict";

	var script = document.currentScript;
	var card = document.getElementById(
		"family-login-reminder-setup"
	);
	var button = document.getElementById(
		"family-login-reminder-enable"
	);
	var message = document.getElementById(
		"family-login-reminder-message"
	);

	if (!script || !card || !button) {
		return;
	}

	var contextPath =
		script.getAttribute("data-context-path");
	var subscriptionUrl =
		script.getAttribute("data-subscription-url");
	var csrfToken =
		script.getAttribute("data-csrf-token");
	var publicKey =
		script.getAttribute("data-vapid-public-key");

	var registration = null;
	var applicationServerKey = null;
	var updating = false;

	function showMessage(text) {
		if (message) {
			message.textContent = text;
		}
	}

	/* 公開鍵をバイト配列に変換する */
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
			throw new Error(
				"公開鍵の形式が正しくありません。"
			);
		}

		return bytes;
	}

	/* 安否通知のON／OFFを変更せずに保存する */
	function saveSubscription(subscription) {
		var data = subscription.toJSON();

		if (!data.endpoint || !data.keys
			|| !data.keys.p256dh || !data.keys.auth) {
			return Promise.reject(new Error(
				"通知の登録情報を取得できませんでした。"
			));
		}

		var params = new URLSearchParams();

		params.set("csrfToken", csrfToken);
		params.set("purpose", "LOGIN_REMINDER");
		params.set("endpoint", data.endpoint);
		params.set("p256dh", data.keys.p256dh);
		params.set("auth", data.keys.auth);

		return fetch(subscriptionUrl, {
			method: "POST",
			credentials: "same-origin",
			redirect: "error",
			headers: {
				"Content-Type":
					"application/x-www-form-urlencoded;charset=UTF-8"
			},
			body: params.toString()
		})
			.then(function(response) {
				if (response.status === 204) {
					return;
				}

				var text;

				if (response.status === 401) {
					text = "ログイン画面から再ログインしてください。";
				} else if (response.status === 403) {
					text = "画面を再読み込みしてください。";
				} else if (response.status === 409) {
					text = "通知登録を保存できませんでした。" +
						"別のアカウントで使用されていないか確認してください。";
				} else if (response.status === 410) {
					text = "通知の登録情報が期限切れです。";
				} else {
					text = "通知登録に失敗しました。（HTTP " +
						response.status + "）";
				}

				var error = new Error(text);
				error.status = response.status;
				throw error;
			});
	}

	function createSubscription() {
		return registration.pushManager.subscribe({
			userVisibleOnly: true,
			applicationServerKey: applicationServerKey
		});
	}

	/* 期限切れの場合だけ一度再登録する */
	function saveWithRecovery(subscription) {
		return saveSubscription(subscription)
			.catch(function(error) {
				if (error.status !== 410) {
					throw error;
				}

				return subscription.unsubscribe()
					.then(function() {
						return registration.pushManager.getSubscription();
					})
					.then(function(remainingSubscription) {
						if (remainingSubscription) {
							throw new Error(
								"古い通知登録を解除できませんでした。" +
								"画面を再読み込みしてください。"
							);
						}

						return createSubscription();
					})
					.then(function(newSubscription) {
						return saveSubscription(newSubscription);
					});
			});
	}

	/* 保存に成功した場合だけ案内を非表示にする */
	function hideSetup() {
		var homeUrl = script.getAttribute("data-home-url");

		if (homeUrl) {
			window.location.replace(homeUrl);
			return;
		}
		if (card.contains(document.activeElement)) {
			var heading = document.querySelector("main h1");

			if (heading) {
				heading.setAttribute("tabindex", "-1");
				heading.focus();
			}
		}

		card.hidden = true;
	}

	function finish() {
		updating = false;
		button.disabled = false;
	}

	function showError(error) {
		showMessage(
			error.message ||
			"通知登録に失敗しました。もう一度お試しください。"
		);
	}

	if (!window.isSecureContext
		|| !("serviceWorker" in navigator)
		|| !("PushManager" in window)
		|| !("Notification" in window)) {
		showMessage(
			"この環境ではプッシュ通知を利用できません。"
		);
		return;
	}

	if (!subscriptionUrl || !csrfToken || !publicKey) {
		showMessage(
			"通知に必要な設定を確認してください。"
		);
		return;
	}

	try {
		applicationServerKey = convertPublicKey(publicKey);
	} catch (error) {
		showMessage("通知用の公開鍵を確認してください。");
		return;
	}

	/* 許可要求はユーザーのクリックを起点にする */
	button.addEventListener("click", function() {
		if (updating || !registration) {
			return;
		}

		if (Notification.permission === "denied") {
			showMessage(
				"通知がブロックされています。" +
				"ブラウザーのサイト設定で通知を許可し、" +
				"画面を再読み込みしてください。"
			);
			return;
		}

		updating = true;
		button.disabled = true;

		var permissionPromise;

		try {
			permissionPromise =
				Notification.permission === "granted"
					? Promise.resolve("granted")
					: Notification.requestPermission();
		} catch (error) {
			showError(error);
			finish();
			return;
		}

		permissionPromise
			.then(function(permission) {
				if (permission !== "granted") {
					throw new Error(
						"通知が許可されていません。" +
						"ブラウザーのサイト設定を確認してください。"
					);
				}

				showMessage("通知の登録情報を保存しています...");

				return registration.pushManager.getSubscription();
			})
			.then(function(subscription) {
				return subscription || createSubscription();
			})
			.then(saveWithRecovery)
			.then(hideSetup)
			.catch(showError)
			.then(finish);
	});

	/* 共通のService Workerを使用する */
	navigator.serviceWorker.register(
		contextPath + "/sw.js",
		{ scope: contextPath + "/" }
	)
		.then(function() {
			return navigator.serviceWorker.ready;
		})
		.then(function(readyRegistration) {
			registration = readyRegistration;

			if (Notification.permission === "denied") {
				showMessage(
					"通知がブロックされています。" +
					"ブラウザーのサイト設定で通知を許可してください。"
				);
				return;
			}

			if (Notification.permission !== "granted") {
				showMessage(
					"「通知を設定する」を押して、通知を許可してください。"
				);
				return;
			}

			updating = true;

			// 登録済みの場合は現在のログイントークンへ関連付け直す
			return registration.pushManager.getSubscription()
				.then(function(subscription) {
					if (!subscription) {
						showMessage(
							"この端末の通知登録が必要です。" +
							"「通知を設定する」を押してください。"
						);
						return;
					}

					return saveWithRecovery(subscription)
						.then(hideSetup);
				});
		})
		.then(finish)
		.catch(function(error) {
			showError(error);

			if (registration) {
				finish();
			}
		});
})();