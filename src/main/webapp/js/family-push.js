/* 保護者向け通知設定 */
(function() {
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
			.then(function(response) {
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
				// 期限切れの登録は呼び出し元で再登録する
				if (response.status === 410) {
					var expiredError = new Error(
						"通知の登録情報が期限切れです。"
					);

					expiredError.status = 410;
					throw expiredError;
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

	// 通知登録情報をサーバーへ保存する
	function saveSubscription(subscription) {
		var data = subscription.toJSON();

		if (!data.endpoint
			|| !data.keys
			|| !data.keys.p256dh
			|| !data.keys.auth) {
			return Promise.reject(new Error(
				"通知の登録情報を取得できませんでした。"
			));
		}

		var params = new URLSearchParams();

		params.set("csrfToken", csrfToken);
		params.set("endpoint", data.endpoint);
		params.set("p256dh", data.keys.p256dh);
		params.set("auth", data.keys.auth);

		return post(subscriptionUrl, params);
	}

	// 新しい通知登録を作成する
	function createSubscription() {
		return registration.pushManager.subscribe({
			userVisibleOnly: true,
			applicationServerKey: applicationServerKey
		});
	}

	// 通知の利用許可を確認し、登録情報を保存する
	function enableNotifications(askPermission) {
		if (!registration || !applicationServerKey) {
			return Promise.reject(new Error(
				"通知の準備が完了していません。"
			));
		}

		var permissionPromise;

		if (Notification.permission === "granted") {
			permissionPromise = Promise.resolve("granted");

		} else if (askPermission) {
			// ユーザーのクリック時のみ許可を求める
			permissionPromise = Notification.requestPermission();

		} else {
			return Promise.reject(new Error(
				"このブラウザーでは通知が許可されていません。" +
				"サイト設定で通知を許可してください。"
			));
		}

		return permissionPromise
			.then(function(permission) {
				if (permission !== "granted") {
					throw new Error(
						"通知が許可されていません。" +
						"ブラウザーのサイト設定を確認してください。"
					);
				}

				return registration.pushManager.getSubscription();
			})
			.then(function(subscription) {
				if (subscription) {
					return subscription;
				}

				return createSubscription();
			})
			.then(function(subscription) {
				return saveSubscription(subscription)
					.catch(function(error) {

						// 期限切れ以外のエラーでは登録を変更しない
						if (error.status !== 410) {
							throw error;
						}

						// 期限切れの登録を解除して一度だけ再登録する
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
								if (newSubscription.endpoint
									=== subscription.endpoint) {
									throw new Error(
										"通知登録を更新できませんでした。" +
										"ブラウザーのサイト設定を確認してください。"
									);
								}

								// 再登録後の失敗はそのまま表示し、繰り返さない
								return saveSubscription(newSubscription);
							});
					});
			});
	}
	// 必須設定を確認する
	if (!updateUrl || !csrfToken) {
		toggle.disabled = true;
		showMessage("通知設定を利用できません。");
		return;
	}

	// 保存中の連続操作を防止する
	toggle.addEventListener("click", function() {
		if (updating) {
			return;
		}

		var nextEnabled =
			toggle.getAttribute("aria-checked") !== "true";

		updating = true;
		toggle.disabled = true;
		showMessage("設定を保存しています...");

		var operation = nextEnabled
			? enableNotifications(true)
			: disableNotifications();

		operation
			.then(function() {
				updateDisplay(nextEnabled);

				showMessage(nextEnabled
					? "通知をONにしました。"
					: "通知をOFFにしました。");
			})
			.catch(function(error) {
				// endpointや認証情報はログに出力しない
				showMessage(
					error.message || "設定を保存できませんでした。"
				);
			})
			.then(function() {
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
		.then(function() {
			return navigator.serviceWorker.ready;
		})
		.then(function(readyRegistration) {
			registration = readyRegistration;

			// OFFの場合や操作中の場合は自動登録しない
			if (toggle.getAttribute("aria-checked") !== "true"
				|| updating) {
				return;
			}

			// 自動確認では通知の許可画面を表示しない
			if (Notification.permission !== "granted") {
				showMessage(
					"通知設定はONですが、このブラウザーでは" +
					"通知が許可されていません。" +
					"サイト設定で通知を許可してください。"
				);
				return;
			}

			updating = true;
			toggle.disabled = true;
			showMessage("通知の登録情報を確認しています...");

			return enableNotifications(false)
				.then(function() {
					showMessage("通知の登録情報を確認しました。");
				})
				.catch(function(error) {
					// endpointや認証情報はログに出力しない
					showMessage(
						"通知の登録情報を確認できませんでした。" +
						(error.message || "") +
						"再読み込みして、もう一度お試しください。"
					);
				})
				.then(function() {
					updating = false;
					toggle.disabled = false;
				});
		})
		.catch(function() {
			showMessage(
				"通知の準備に失敗しました。再読み込みしてください。"
			);
		});
})();