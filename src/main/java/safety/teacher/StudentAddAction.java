package safety.teacher;

import java.time.LocalDate;
import java.time.ZoneId;

import bean.User;
import dao.TeacherClassDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/** 登録済みの生徒を担当クラスに追加する。 */
public class StudentAddAction extends Action {
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        res.setHeader("Cache-Control", "no-store");
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/safety/auth/Login.action");
            return;
        }
        if (!user.isActive() || !"TEACHER".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        int year = LocalDate.now(ZoneId.of("Asia/Tokyo")).minusMonths(3).getYear();
        TeacherClassDao dao = new TeacherClassDao();
        boolean done = false;
        if ("POST".equals(req.getMethod())) {
            String token = (String) session.getAttribute("csrfToken");
            if (token == null || !token.equals(req.getParameter("csrfToken"))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            long classId;
            try {
                classId = Long.parseLong(req.getParameter("classId"));
            } catch (NumberFormatException ex) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            String input = req.getParameter("studentLoginId");
            String loginId = input == null ? "" : input.strip();
            String result = loginId.isEmpty() ? "EMPTY"
                    : dao.addRegisteredStudent(user.getId(), year, classId, loginId);
            if ("FORBIDDEN".equals(result)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if ("ADDED".equals(result)) {
                session.setAttribute("studentAddDone", Boolean.TRUE);
                res.setStatus(HttpServletResponse.SC_SEE_OTHER);
                res.setHeader("Location", req.getContextPath() + "/safety/teacher/StudentAdd.action?done=1");
                return;
            }
            String error = switch (result) {
                case "EMPTY" -> "生徒IDを入力してください。";
                case "UNKNOWN" -> "該当する生徒IDが見つかりません。";
                case "ALREADY" -> "この生徒は既にこのクラスに登録されています。";
                default -> "この生徒は今年度のクラスに既に登録されています。";
            };
            req.setAttribute("addError", error);
            req.setAttribute("selectedClassId", classId);
            req.setAttribute("studentLoginId", loginId);
        } else if ("GET".equals(req.getMethod())) {
            done = "1".equals(req.getParameter("done"))
                    && Boolean.TRUE.equals(session.getAttribute("studentAddDone"));
            session.removeAttribute("studentAddDone");
        } else {
            res.setHeader("Allow", "GET, POST");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        req.setAttribute("showGroups", true);
        req.setAttribute("assignedClasses", dao.findAssignedClasses(user.getId(), year));
        String page = done ? "staff_student_add_done.jsp" : "staff_student_add.jsp";
        req.getRequestDispatcher("/WEB-INF/views/teacher/" + page).forward(req, res);
    }
}
