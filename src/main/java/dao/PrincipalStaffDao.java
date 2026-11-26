package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;

/** 職員一覧を取得する。 */
public class PrincipalStaffDao extends DAO {
    public List<StaffItem> findStaff() throws NamingException, SQLException {
        String sql = """
                SELECT u.id, u.name, u.login_id,
                       CASE WHEN u.active THEN '利用中' ELSE '停止中' END AS status,
                       COALESCE(STRING_AGG(DISTINCT c.class_name, '、' ORDER BY c.class_name),
                                '未割り当て') AS assigned_classes
                FROM public.users u
                LEFT JOIN public.teacher_class tc ON tc.teacher_user_id = u.id
                LEFT JOIN public.school_class c ON c.id = tc.class_id
                    AND c.school_year = (SELECT MAX(school_year) FROM public.school_class)
                WHERE u.role = 'TEACHER'
                GROUP BY u.id, u.name, u.login_id, u.active
                ORDER BY u.id
                """;
        List<StaffItem> items = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(new StaffItem(rs.getLong("id"), rs.getString("name"),
                    rs.getString("login_id"), rs.getString("status"), rs.getString("assigned_classes")));
            }
        }
        return items;
    }

    /** 校長による職員登録。パスワードはハッシュ済みの値のみを受け取る。 */
    public boolean createStaff(long principalId, String name, String loginId, String passwordHash)
            throws NamingException, SQLException {
        String sql = """
                INSERT INTO public.users (name, login_id, password_hash, role, active)
                SELECT ?, ?, ?, 'TEACHER', TRUE
                FROM public.users
                WHERE id = ? AND role = 'PRINCIPAL' AND active = TRUE
                ON CONFLICT (login_id) DO NOTHING
                """;
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, loginId);
            ps.setString(3, passwordHash);
            ps.setLong(4, principalId);
            return ps.executeUpdate() == 1;
        }
    }

    /** 職員の編集情報を取得する。 */
    public StaffItem findById(long id) throws NamingException, SQLException {
        String sql = "SELECT id, name, login_id, active FROM public.users WHERE id = ? AND role = 'TEACHER'";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new StaffItem(rs.getLong("id"), rs.getString("name"),
                    rs.getString("login_id"), rs.getBoolean("active") ? "利用中" : "停止中", "") : null;
            }
        }
    }

    /** 利用停止と担当クラス解除は同一トランザクションで行う。 */
    public boolean updateStaff(long principalId, long teacherId, String name,
            String passwordHash, boolean active) throws NamingException, SQLException {
        String sql = """
                UPDATE public.users SET name = ?, active = ?, password_hash = COALESCE(?, password_hash)
                WHERE id = ? AND role = 'TEACHER'
                  AND EXISTS (SELECT 1 FROM public.users p WHERE p.id = ? AND p.role = 'PRINCIPAL' AND p.active = TRUE)
                """;
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setBoolean(2, active);
                    ps.setString(3, passwordHash);
                    ps.setLong(4, teacherId);
                    ps.setLong(5, principalId);
                    if (ps.executeUpdate() != 1) { con.rollback(); return false; }
                }
                if (!active) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "DELETE FROM public.teacher_class WHERE teacher_user_id = ?")) {
                        ps.setLong(1, teacherId);
                        ps.executeUpdate();
                    }
                }
                con.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                try { con.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); throw e; }
                throw e;
            }
        }
    }

    public static final class StaffItem {
        private final long id;
        private final String name, loginId, status, assignedClasses;
        public StaffItem(long id, String name, String loginId, String status, String assignedClasses) {
            this.id = id; this.name = name; this.loginId = loginId;
            this.status = status; this.assignedClasses = assignedClasses;
        }
        public long getId() { return id; }
        public String getName() { return name; }
        public String getLoginId() { return loginId; }
        public String getStatus() { return status; }
        public String getAssignedClasses() { return assignedClasses; }
    }
}
