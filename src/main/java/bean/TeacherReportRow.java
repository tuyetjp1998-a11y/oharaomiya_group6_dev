package bean;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** 職員ホームに表示する生徒の最新回答。 */
public class TeacherReportRow {
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneId.of("Asia/Tokyo"));

    private final long studentId;
    private final String loginId;
    private final long reportId;
    private final String name;
    private final String className;
    private final String status;
    private final String dangerType;
    private final Instant reportedAt;

    public TeacherReportRow(long studentId, String loginId, long reportId, String name, String className,
            String status, String dangerType, Instant reportedAt) {
        this.studentId = studentId;
        this.loginId = loginId;
        this.reportId = reportId;
        this.name = name;
        this.className = className;
        this.status = status;
        this.dangerType = dangerType;
        this.reportedAt = reportedAt;
    }

    public String getLoginId() { return loginId; }
    public long getStudentId() { return studentId; }
    public long getReportId() { return reportId; }
    public String getName() { return name; }
    public String getClassName() { return className; }
    public String getStatus() { return status; }
    public Instant getReportedAt() { return reportedAt; }
    public String getDisplayTime() { return reportedAt == null ? "—" : DISPLAY_TIME.format(reportedAt); }
    public String getStatusLabel() { return statusLabel(status, dangerType); }

    static String statusLabel(String status, String dangerType) {
        if (status == null) {
            return "未回答";
        }
        if ("SAFE".equals(status)) {
            return "無事";
        }
        if (!"WORRIED".equals(status) && !"DANGER".equals(status)) {
            return "不明";
        }
        if ("DANGER".equals(status)) {
            return "危険";
        }
        String type = switch (dangerType == null ? "" : dangerType) {
            case "ACCIDENT" -> "事故";
            case "EARTHQUAKE" -> "地震";
            case "FIRE" -> "火災";
            case "LANDSLIDE" -> "土砂崩れ";
            case "HEATSTROKE" -> "熱中症";
            default -> "";
        };
        return type.isEmpty() ? "困っている" : "困っている（" + type + "）";
    }
}
