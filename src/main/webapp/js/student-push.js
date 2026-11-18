/* 生徒のログイン期限通知を登録する */
(function() {
	"use strict";

	var script = document.currentScript;
	var button = document.getElementById(
		"student-notification-enable"
	);
	var message = document.getElementById(
		"student-notification-message"
	);

	if (!script || !button) {
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

	/* 登録完了後はホームの初回案内を非表示にする */
	function completeSetup(text) {
		var homeUrl = script.getAttribute("data-home-url");

		if (homeUrl) {
			window.location.replace(homeUrl);
			return;
		}
		button.textContent = "通知登録を更新する";
		showMessage(text);

		var setup = document.getElementById(
			"student-notification-setup"
		);

		if (setup) {
			// 非表示にする案内内にフォーカスを残さない
			if (setup.contains(document.activeElement)) {
				var answerLink = document.querySelector(
					".answer-area .answer-button"
				);

				if (answerLink) {
					answerLink.focus();
				}
			}

			setup.hidden = true;
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
			throw new Error("公開鍵の形式が正しくありません。");
		}

		return bytes;
	}

	/* 登録情報をサーバーへ保存する */
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

				var error;

				if (response.status === 401) {
					error = new Error(
						"ログイン画面から再ログインしてください。"
					);
				} else if (response.status === 403) {
					error = new Error(
						"画面を再読み込みしてください。"
					);
				} else if (response.status === 409) {
					error = new Error(
						"通知登録を保存できませんでした。" +
						"別のアカウントで使用されていないか確認してください。"
					);
				} else if (response.status === 410) {
					error = new Error(
						"通知の登録情報が期限切れです。"
					);
				} else {
					error = new Error(
						"通知登録に失敗しました。（HTTP " +
						response.status + "）"
					);
				}

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

	/* 期限切れの場合だけ、一度再登録する */
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
								"古い通知登録を解除できませんでした。"
							);
						}

						return createSubscription();
					})
					.then(function(newSubscription) {
						if (newSubscription.endpoint === subscription.endpoint) {
							throw new Error(
								"通知登録を更新できませんでした。"
							);
						}

						return saveSubscription(newSubscription);
					});
			});
	}

	function finish() {
		updating = false;
		button.disabled = false;
	}

	/* クリック時に通知の許可を求める */
	button.addEventListener("click", function() {
		// ブロック済みの場合は設定変更の方法を案内する
		if (Notification.permission === "denied") {
			showMessage(
				"通知がブロックされています。" +
				"端末・ブラウザーのサイト設定で通知を許可し、" +
				"この画面を再読み込みしてください。"
			);
			return;
		}

		updating = true;
		button.disabled = true;

		// 許可要求はユーザーのクリックを起点にする
		var permissionPromise =
			Notification.permission === "granted"
				? Promise.resolve("granted")
				: Notification.requestPermission();

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
			.then(function() {
				completeSetup("通知の登録情報を保存しました。");
			})
			.catch(function(error) {
				showMessage(
					error.message || "通知登録に失敗しました。"
				);
			})
			.then(finish);
	});

	if (!window.isSecureContext
		|| !("serviceWorker" in navigator)
		|| !("PushManager" in window)
		|| !("Notification" in window)) {
		showMessage("この環境ではプッシュ通知を利用できません。");
		return;
	}

	if (!subscriptionUrl || !csrfToken || !publicKey) {
		showMessage("通知に必要な設定を確認してください。");
		return;
	}

	try {
		applicationServerKey = convertPublicKey(publicKey);
	} catch (error) {
		showMessage("通知用の公開鍵を確認してください。");
		return;
	}

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

			// 許可済み・登録済みの場合は現在のトークンへ関連付け直す
			if (Notification.permission === "denied") {
				showMessage(
					"通知がブロックされています。" +
					"端末・ブラウザーのサイト設定で通知を許可し、" +
					"この画面を再読み込みしてください。"
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

			return registration.pushManager.getSubscription()
				.then(function(subscription) {
					if (!subscription) {
						showMessage(
							"通知の許可は完了しています。" +
							"ボタンを押して、この端末の通知登録を完了してください。"
						);
						return;
					}

					return saveWithRecovery(subscription)
						.then(function() {
							completeSetup("通知の登録情報を確認しました。");
						});
				});
		})
		.then(finish)
		.catch(function(error) {
			showMessage(
				error.message || "通知の準備に失敗しました。"
			);

			if (registration) {
				finish();
			}
		});
})();