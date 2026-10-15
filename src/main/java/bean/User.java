package bean;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 利用者情報を保持するJavaBean。
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    // 利用者ID
    private long id;

    // ログインID
    private String loginId;

    // 氏名
    private String name;

    // 利用者区分：STUDENT / FAMILY / TEACHER / PRINCIPAL
    private String role;

    // アカウントの有効状態
    private boolean active;

    // 登録日時
    private OffsetDateTime createdAt;
    
    // 位置情報の利用設定
    private boolean locationEnabled;
    public User() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isLocationEnabled() {
        return locationEnabled;
    }

    public void setLocationEnabled(boolean locationEnabled) {
        this.locationEnabled = locationEnabled;
    }
}