package bean;

import java.io.Serializable;

/**
 * 保護者ホームに表示する生徒情報。
 */
public class FamilyStudent implements Serializable {

    private static final long serialVersionUID = 1L;

    // 生徒の利用者ID
    private long studentUserId;

    // 生徒の氏名
    private String studentName;

    // 最新の安否状況：SAFE / DANGER / 未回答の場合はnull
    private String status;
    
    // 生徒のログインID
    private String studentLoginId;

    public FamilyStudent() {
    }

    public long getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(long studentUserId) {
        this.studentUserId = studentUserId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getStudentLoginId() {
        return studentLoginId;
    }

    public void setStudentLoginId(String studentLoginId) {
        this.studentLoginId = studentLoginId;
    }
}