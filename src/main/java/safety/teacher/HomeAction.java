package safety.teacher;

import java.time.LocalDate;
import java.time.ZoneId;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import bean.TeacherClassSummary;
import bean.TeacherReportRow;
import bean.User;
import dao.TeacherClassDao;
import dao.SafetyReportDao;
import bean.SafetyReport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class HomeAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        // ローカル変数の宣言 1
        HttpSession session = req.getSession(false);
        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        // リクエストパラメーターの取得 2
        // なし

        // HTTPメソッドの確認
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        
        // ビジネスロジック 4
        if (user == null) {
            res.sendRedirect(req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }

        if (!user.isActive()
                || !"TEACHER".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 日本の学校年度（4月始まり）に合わせて担当クラスを取得する。
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        int schoolYear = today.minusMonths(3).getYear();
        List<TeacherClassSummary> classes =
                new TeacherClassDao().findAssignedClasses(user.getId(), schoolYear);
        List<TeacherReportRow> reports = new ArrayList<>();
        long total = 0;
        long answered = 0;
        long attention = 0;
        for (TeacherClassSummary room : classes) {
            reports.addAll(room.getReports());
            total += room.getStudentCount();
            answered += room.getAnsweredCount();
            attention += room.getAttentionCount();
        }
        reports.sort(Comparator.comparing(TeacherReportRow::getReportedAt)
                .thenComparingLong(TeacherReportRow::getReportId).reversed());
        req.setAttribute("latestReports", reports);
        req.setAttribute("todayLabel", today.format(DateTimeFormatter.ofPattern("MM月dd日")));
        boolean showGroups = "groups".equals(req.getParameter("view"));
        req.setAttribute("showGroups", showGroups);
        String page = "home.jsp";
        if (showGroups) {
            page = "staff_groups.jsp";
        }
        if ("groups".equals(req.getParameter("view")) && "removed".equals(req.getParameter("done"))) {
            if (Boolean.TRUE.equals(session.getAttribute("studentRemoved"))) {
                page = "staff_groups_done.jsp";
            }
            session.removeAttribute("studentRemoved");
        }
        boolean showRoster = "answers".equals(req.getParameter("view"));
        if (showRoster) {
            page = "staff_answers.jsp";
        }
        if (showGroups && req.getParameter("classId") != null) {
            long classId;
            try {
                classId = Long.parseLong(req.getParameter("classId"));
            } catch (NumberFormatException ex) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            TeacherClassSummary room = classes.stream()
                    .filter(item -> item.getClassId() == classId).findFirst().orElse(null);
            if (room == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            req.setAttribute("detailClass", room);
            page = "staff_group_detail.jsp";
        }
        String selectedId = req.getParameter("studentId");
        if (showRoster && selectedId != null) {
            long studentId;
            try {
                studentId = Long.parseLong(selectedId);
            } catch (NumberFormatException ex) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            // ログイン中の教員が今年度担当する生徒のみ選択できる。
            TeacherReportRow student = classes.stream()
                    .flatMap(room -> room.getStudents().stream())
                    .filter(row -> row.getStudentId() == studentId)
                    .findFirst().orElse(null);
            if (student == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            req.setAttribute("detailStudent", student);
            page = "staff_person.jsp";
            if (student.getStatus() != null) {
                SafetyReport report = new SafetyReportDao().get(student.getReportId(), studentId);
                if (report == null) {
                    // 取得中に回答が取り消された場合は一覧を再取得する。
                    res.sendRedirect(req.getContextPath() + "/safety/teacher/Home.action?view=answers");
                    return;
                }
                req.setAttribute("detailReport", report);
            }
        }
        req.setAttribute("showRoster", showRoster);
        req.setAttribute("tableRows", showRoster
                ? classes.stream().flatMap(room -> room.getStudents().stream()).toList()
                : reports);
        req.setAttribute("assignedClasses", classes);
        req.setAttribute("studentTotal", total);
        req.setAttribute("answered", answered);
        req.setAttribute("unanswered", total - answered);
        req.setAttribute("attention", attention);
        req.setAttribute("answerPercent", total == 0 ? 0 : Math.round(100.0 * answered / total));

        // レスポンス値をセット 6
        res.setHeader("Cache-Control", "no-store");
        req.setAttribute("pageTitle", "教員トップ");

        // JSPへフォワード 7
        req.getRequestDispatcher("/WEB-INF/views/teacher/" + page)
                .forward(req, res);
    }
}