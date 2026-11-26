package safety.principal;

import bean.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;
import dao.SafetyDispatchDao;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.sql.SQLException;
import javax.naming.NamingException;

public class DistributionSubmitAction extends Action {
    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        req.setCharacterEncoding("UTF-8");
        res.setHeader("Cache-Control", "no-store");
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/safety/auth/Login.action");
            return;
        }
        if (!user.isActive() || !"PRINCIPAL".equals(user.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            res.setHeader("Allow", "POST");
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        String expectedToken = (String) session.getAttribute("csrfToken");
        if (expectedToken == null || !expectedToken.equals(req.getParameter("csrfToken"))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String kind = req.getParameter("kind");
        String disaster = req.getParameter("disaster");
        String target = req.getParameter("target");
        String title = req.getParameter("title");
        String message = req.getParameter("message");
        String deadlineText = req.getParameter("deadline");
        req.setAttribute("kind", kind);
        req.setAttribute("disaster", disaster);
        req.setAttribute("target", target);
        req.setAttribute("dispatchTitle", title);
        req.setAttribute("dispatchMessage", message);
        req.setAttribute("deadline", deadlineText);
        OffsetDateTime deadline = null;
        String error = null;
        if ("SAFETY".equals(kind)) {
            // 安否確認は全生徒のみ。入力された件名・本文は使用しない。
            target = "STUDENT";
            String label = switch (disaster == null ? "" : disaster) {
                case "EARTHQUAKE" -> "地震";
                case "TYPHOON" -> "台風";
                case "FLOOD" -> "大雨・洪水";
                case "TSUNAMI" -> "津波";
                case "FIRE" -> "火災";
                case "LANDSLIDE" -> "土砂災害";
                case "OTHER" -> "その他";
                default -> null;
            };
            if (label == null) {
                error = "災害の種類を選択してください。";
            } else {
                title = "その他".equals(label) ? "緊急時の安否確認" : label + "に伴う安否確認";
                message = ("その他".equals(label) ? "緊急事態に伴い、" : label + "に伴い、")
                        + "現在の安否状況を回答してください。";
                try {
                    deadline = LocalDateTime.parse(deadlineText == null ? "" : deadlineText)
                            .atZone(ZoneId.of("Asia/Tokyo")).toOffsetDateTime();
                    if (!deadline.isAfter(OffsetDateTime.now())) {
                        error = "回答期限は現在より後の日時を指定してください。";
                    }
                } catch (DateTimeException e) {
                    error = "回答期限を正しく入力してください。";
                }
            }
        } else if ("NORMAL".equals(kind)) {
            if (!("STUDENT".equals(target) || "FAMILY".equals(target)
                    || "TEACHER".equals(target) || "ALL".equals(target))) {
                error = "配信対象を選択してください。";
            } else if (title == null || title.isBlank() || title.length() > 100 || title.indexOf(0) >= 0) {
                error = "タイトルを1文字以上100文字以内で入力してください。";
            } else if (message == null || message.isBlank() || message.length() > 2000 || message.indexOf(0) >= 0) {
                error = "メッセージを1文字以上2000文字以内で入力してください。";
            }
        } else {
            error = "配信種類を選択してください。";
        }
        if (error == null) {
            try {
                long dispatchId = new SafetyDispatchDao().create(user.getId(), target,
                        title, message, deadline);
                // DB登録完了後のみ通知を送信する。
                tool.DispatchPushService.send(dispatchId, deadline != null);
                session.setAttribute("dispatchDoneId", dispatchId);
                session.setAttribute("dispatchDoneDeadline", deadline == null ? null : deadline.format(
                        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
                res.setStatus(HttpServletResponse.SC_SEE_OTHER);
                res.setHeader("Location", req.getContextPath()
                        + "/safety/principal/DistributionDone.action");
                return;
            } catch (SecurityException e) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            } catch (IllegalArgumentException | IllegalStateException e) {
                error = e.getMessage();
            } catch (SQLException | NamingException e) {
                req.getServletContext().log("安否確認配信の登録に失敗しました。", e);
                error = "配信の登録に失敗しました。管理者にお問い合わせください。";
            }
        }
        req.setAttribute("dispatchError", error);
        req.getRequestDispatcher("/WEB-INF/views/principal/distribution.jsp")
                .forward(req, res);
    }
}
