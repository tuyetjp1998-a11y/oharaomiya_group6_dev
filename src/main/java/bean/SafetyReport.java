package bean;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 安否回答情報を保持するJavaBean。
 */
public class SafetyReport implements Serializable {

    private static final long serialVersionUID = 1L;

    // 回答ID
    private long id;

    // 回答者の利用者ID
    private long userId;

    // 安否状況：SAFE / DANGER
    private String status;

    // 危険の種類（無事の場合はnull）
    private String dangerType;

    // 回答日時
    private OffsetDateTime reportedAt;

    public SafetyReport() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDangerType() {
        return dangerType;
    }

    public void setDangerType(String dangerType) {
        this.dangerType = dangerType;
    }

    public OffsetDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(OffsetDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}