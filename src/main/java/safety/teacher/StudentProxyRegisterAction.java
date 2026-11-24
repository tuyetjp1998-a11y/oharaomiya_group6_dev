package safety.teacher;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;

import bean.User;
import dao.TeacherClassDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;
import tool.PasswordUtil;

/** 教員が生徒アカウントを代理登録する。 */
public class StudentProxyRegisterAction extends Action {
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
        String page = "staff_student_proxy_register.jsp";

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String expectedToken = (String) session.getAttribute("csrfToken");
            if (expectedToken == null
                    || !expectedToken.equals(req.getParameter("csrfToken"))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            String name = strip(req.getParameter("name"));
            String loginId = strip(req.getParameter("loginId"));
            String password = req.getParameter("password");
            String classIdText = req.getParameter("classId");
            long classId = parseClassId(classIdText);

            String error = validate(name, loginId, password, classId);
            if (error == null) {
                String result = dao.registerStudentByProxy(
                        user.getId(), year, classId, loginId, name,
                        PasswordUtil.hash(password));
                if ("FORBIDDEN".equals(result)) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                if ("ADDED".equals(result)) {
                    session.setAttribute("studentProxyRegisterDone", Boolean.TRUE);
                    res.setStatus(HttpServletResponse.SC_SEE_OTHER);
                    res.setHeader("Location", req.getContextPath()
                            + "/safety/teacher/StudentProxyRegister.action?done=1");
                    return;
                }
                error = "DUPLICATE".equals(result)
                        ? "このユーザーIDは既に使用されています。"
                        : "生徒を登録できませんでした。";
            }

            req.setAttribute("proxyRegisterError", error);
            req.setAttribute("inputName", name);
            req.setAttribute("inputLoginId", loginId);
            req.setAttribute("selectedClassId", classIdText);
            // パスワードは画面やセッションに保存しない。
        } else if ("GET".equalsIgnoreCase(req.getMethod())) {
            boolean done = "1".equals(req.getParameter("done"))
                    && Boolean.TRUE.equals(
                            session.getAttribute("studentProxyRegisterDone"));
            session.removeAttribute("studentProxyRegisterDone");
            if (done) {
                page = "staff_student_proxy_register_done.jsp";
            }
        } else {
            res.setHeader("Allow", "GET, POST");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        req.setAttribute("showGroups", true);
        req.setAttribute("assignedClasses", dao.findAssignedClasses(user.getId(), year));
        req.getRequestDispatcher("/WEB-INF/views/teacher/" + page).forward(req, res);
    }

    private static String strip(String value) {
        return value == null ? "" : value.strip();
    }

    private static long parseClassId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String validate(
            String name, String loginId, String password, long classId) {
        if (name.isBlank() || name.length() > 40) {
            return "氏名は1～40文字で入力してください。";
        }
        if (loginId.isBlank() || loginId.length() > 40) {
            return "ユーザーIDは1～40文字で入力してください。";
        }
        if (password == null || password.isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return "初期パスワードはUTF-8で1～72バイトにしてください。";
        }
        if (classId <= 0) {
            return "担当クラスを選択してください。";
        }
        return null;
    }
}
