package bean;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 教員の承認画面に表示する申請情報。
 */
public class FamilyRequestReview implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private String familyName;
    private String familyLoginId;
    private String studentName;
    private String studentLoginId;
    private String className;
    private OffsetDateTime requestedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getFamilyLoginId() {
        return familyLoginId;
    }

    public void setFamilyLoginId(String familyLoginId) {
        this.familyLoginId = familyLoginId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentLoginId() {
        return studentLoginId;
    }

    public void setStudentLoginId(String studentLoginId) {
        this.studentLoginId = studentLoginId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(OffsetDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getRequestedAtText() {
        return requestedAt == null
                ? ""
                : requestedAt
                        .atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
                        .format(DateTimeFormatter.ofPattern(
                                "yyyy/MM/dd HH:mm"));
    }
}