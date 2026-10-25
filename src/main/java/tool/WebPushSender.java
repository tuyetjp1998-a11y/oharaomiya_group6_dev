package tool;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Set;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import bean.WebPushSubscription;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;

/**
 * VAPIDを使用してWeb Pushを送信する。
 */
public class WebPushSender {

    // 送信を許可するPushサービス
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "fcm.googleapis.com",
            "updates.push.services.mozilla.com",
            "web.push.apple.com");

    private final PushService pushService;

    public WebPushSender() throws Exception {

        // 暗号化プロバイダーを登録する
        if (Security.getProvider("BC") == null) {
            Security.addProvider(
                    new BouncyCastleProvider());
        }

        // 秘密鍵は環境変数から取得する
        String publicKey = requiredEnvironment(
                "VAPID_PUBLIC_KEY");
        String privateKey = requiredEnvironment(
                "VAPID_PRIVATE_KEY");
        String subject = requiredEnvironment(
                "VAPID_SUBJECT");

        if (!subject.startsWith("mailto:")
                && !subject.startsWith("https://")) {
            throw new IllegalStateException(
                    "VAPID_SUBJECTの形式を確認してください。");
        }

        pushService = new PushService(
                publicKey,
                privateKey,
                subject);
    }

    /**
     * 登録済みのブラウザーへ通知する。
     *
     * @return PushサービスのHTTPステータス
     */
    public int send(WebPushSubscription subscription)
            throws Exception {

        // 任意のURLへ送信しないように送信先を確認する
        URI endpoint = URI.create(
                subscription.getEndpoint());

        if (!"https".equalsIgnoreCase(endpoint.getScheme())
                || endpoint.getHost() == null
                || !ALLOWED_HOSTS.contains(endpoint.getHost())
                || endpoint.getUserInfo() != null
                || endpoint.getFragment() != null
                || (endpoint.getPort() != -1
                    && endpoint.getPort() != 443)) {

            throw new IllegalArgumentException(
                    "許可されていない通知先です。");
        }

        // 通知本文はService Worker側で表示する
        // 生徒の氏名や位置情報は送信しない
        byte[] payload =
                "{}".getBytes(StandardCharsets.UTF_8);

        Notification notification = new Notification(
                subscription.getEndpoint(),
                subscription.getP256dh(),
                subscription.getAuth(),
                payload,
                300);

        HttpPost post = pushService.preparePost(
                notification,
                Encoding.AES128GCM);

        // 通信待ち時間を制限する
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
                .build();

        // リダイレクト先へ認証情報を送信しない
        try (CloseableHttpClient client =
                     HttpClients.custom()
                             .setDefaultRequestConfig(config)
                             .disableRedirectHandling()
                             .disableAutomaticRetries()
                             .build();
             CloseableHttpResponse response =
                     client.execute(post)) {

            return response.getStatusLine()
                    .getStatusCode();
        }
    }

    /**
     * 必須の環境変数を取得する。
     */
    private static String requiredEnvironment(String name) {

        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    name + "が設定されていません。");
        }

        return value.strip();
    }
}