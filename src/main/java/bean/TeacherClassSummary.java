package bean;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** 職員ホームに表示する担当クラスと在籍生徒数。 */
public class TeacherClassSummary {
    private final long classId;
    private final String className;
    private final List<TeacherReportRow> students = new ArrayList<>();
    private final List<TeacherReportRow> reports = new ArrayList<>();
    private final List<AttentionStudent> attentionStudents = new ArrayList<>();

    public TeacherClassSummary(long classId, String className) {
        this.classId = classId;
        this.className = className;
    }

    /** 在籍生徒の最新回答を1件だけ集計する。 */
    public void addStudent(long studentId, String loginId, String name, String status, String dangerType,
            long reportId, OffsetDateTime reportedAt) {
        TeacherReportRow row = new TeacherReportRow(studentId, loginId, reportId, name, className, status,
                dangerType, reportedAt == null ? null : reportedAt.toInstant());
        students.add(row);
        if (status != null) {
            reports.add(row);
        }
        if ("DANGER".equals(status) || "WORRIED".equals(status)) {
            attentionStudents.add(new AttentionStudent(studentId, name, status, dangerType));
        }
    }

    public List<AttentionStudent> getAttentionStudents() {
        return List.copyOf(attentionStudents);
    }

    public List<TeacherReportRow> getStudents() {
        return List.copyOf(students);
    }

    public List<TeacherReportRow> getReports() {
        return List.copyOf(reports);
    }

    public long getClassId() { return classId; }

    public String getClassName() {
        return className;
    }

    public long getStudentCount() {
        return students.size();
    }

    public long getAnsweredCount() {
        return reports.size();
    }

    public long getAttentionCount() {
        return attentionStudents.size();
    }
}
