package bean;

import java.io.Serializable;

/**
 * Web Pushの登録情報を保持するJavaBean。
 */
public class WebPushSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    // 登録ID
    private long id;

    // 保護者の利用者ID
    private long userId;

    // 通知の送信先
    private String endpoint;

    // ブラウザーの暗号化用公開鍵
    private String p256dh;

    // 暗号化に使用する認証情報
    private String auth;

    public WebPushSubscription() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getP256dh() {
        return p256dh;
    }

    public void setP256dh(String p256dh) {
        this.p256dh = p256dh;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }
}