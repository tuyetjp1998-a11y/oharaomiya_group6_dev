package safety.teacher;

import java.time.LocalDate;
import java.time.ZoneId;

import bean.User;
import dao.TeacherClassDao;
import dao.WorriedTypeDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

/** 「困っている」の項目を追加する。 */
public class DangerTypeAddAction extends Action {

    @Override
    public void execute(HttpServletRequest req,
            HttpServletResponse res) throws Exception {

        req.setCharacterEncoding("UTF-8");
        res.setHeader("Cache-Control", "no-store");
        HttpSession session = req.getSession(false);
        User user = session == null
                ? null
                : (User) session.getAttribute("user");

        if (user == null) {
            res.sendRedirect(req.getContextPath()
                    + "/safety/auth/Login.action");
            return;
        }
        if (!user.isActive() || !"TEACHER".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        int schoolYear = LocalDate.now(ZoneId.of("Asia/Tokyo"))
                .minusMonths(3).getYear();
        TeacherClassDao classDao = new TeacherClassDao();

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String expectedToken =
                    (String) session.getAttribute("csrfToken");
            if (expectedToken == null
                    || !expectedToken.equals(
                            req.getParameter("csrfToken"))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            String name = req.getParameter("name");
            name = name == null ? "" : name.strip();

            if (name.isEmpty() || name.length() > 30
                    || name.indexOf('\0') >= 0) {
                req.setAttribute(
                        "addError",
                        "項目名を1文字以上30文字以内で入力してください。");
                req.setAttribute("dangerTypeName", name);
            } else if (!new WorriedTypeDao().add(
                    name, user.getId())) {
                req.setAttribute(
                        "addError",
                        "同じ項目名が既に登録されています。");
                req.setAttribute("dangerTypeName", name);
            } else {
                session.setAttribute("dangerTypeAdded", name);
                res.setStatus(HttpServletResponse.SC_SEE_OTHER);
                res.setHeader(
                        "Location",
                        req.getContextPath()
                                + "/safety/teacher/"
                                + "DangerTypeAdd.action?done=1");
                return;
            }
        } else if (!"GET".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "GET, POST");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String addedName = "1".equals(req.getParameter("done"))
                ? (String) session.getAttribute("dangerTypeAdded")
                : null;
        if (addedName != null) {
            session.removeAttribute("dangerTypeAdded");
            req.setAttribute("addedName", addedName);
        }

        req.setAttribute("showDangerTypes", true);
        req.setAttribute(
                "assignedClasses",
                classDao.findAssignedClasses(
                        user.getId(), schoolYear));

        String page = addedName == null
                ? "danger_type_add.jsp"
                : "danger_type_add_done.jsp";
        req.getRequestDispatcher(
                "/WEB-INF/views/teacher/" + page)
                .forward(req, res);
    }
}
