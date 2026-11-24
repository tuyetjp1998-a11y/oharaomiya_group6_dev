package bean;

/** 担当クラスの要対応一覧に表示する生徒。 */
public class AttentionStudent {
    private final long studentId;
    private final String name;
    private final String status;
    private final String dangerType;

    public AttentionStudent(long studentId, String name, String status, String dangerType) {
        this.studentId = studentId;
        this.name = name;
        this.status = status;
        this.dangerType = dangerType;
    }

    public long getStudentId() {
        return studentId;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return TeacherReportRow.statusLabel(status, dangerType);
    }
}
