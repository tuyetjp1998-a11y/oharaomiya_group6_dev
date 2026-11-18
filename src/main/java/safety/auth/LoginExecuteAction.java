package safety.auth;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

import bean.User;
import dao.LoginTokenDao;
import dao.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;
import tool.LoginTokenUtil;
import tool.PasswordUtil;

public class LoginExecuteAction extends Action {

	@Override
	public void execute(
			HttpServletRequest req,
			HttpServletResponse res) throws Exception {

		// ローカル変数の宣言 1
		HttpSession session = req.getSession(false);
		UserDao userDao = new UserDao();
		UserDao.LoginData loginData = null;
		User user = null;

		boolean loginFailed = true;
		String redirectPath = null;

		// リクエストパラメーターの取得 2
		String loginId = req.getParameter("loginId");
		String password = req.getParameter("password");
		String csrfToken = req.getParameter("csrfToken");

		// POST以外のリクエストを拒否
		if (!"POST".equalsIgnoreCase(req.getMethod())) {
			res.setHeader("Allow", "POST");
			res.sendError(
					HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}

		// CSRFトークンを確認
		String expectedToken = session == null
				? null
				: (String) session.getAttribute("csrfToken");

		if (expectedToken == null
				|| csrfToken == null
				|| !expectedToken.equals(csrfToken)) {

			res.sendError(
					HttpServletResponse.SC_FORBIDDEN,
					"ログイン画面を開き直してください。");
			return;
		}

		// DBからデータ取得 3
		if (loginId != null
				&& !loginId.isBlank()
				&& loginId.length() <= 100
				&& password != null
				&& !password.isEmpty()) {

			loginData = userDao.findByLoginId(loginId);
		}

		// ビジネスロジック 4
		if (loginData != null) {

			user = loginData.getUser();

			boolean passwordCorrect = PasswordUtil.verify(
					password,
					loginData.getPasswordHash());

			if (passwordCorrect && user.isActive()) {

				// DBに保存された利用者区分で遷移先を決定
				switch (user.getRole()) {
				case "STUDENT":
					redirectPath = "/safety/student/Home.action";
					break;

				case "FAMILY":
					redirectPath = "/safety/family/Home.action";
					break;

				case "TEACHER":
					redirectPath = "/safety/teacher/Home.action";
					break;

				case "PRINCIPAL":
					redirectPath = "/safety/principal/Home.action";
					break;

				default:
					redirectPath = null;
					break;
				}

				loginFailed = redirectPath == null;
			}
		}

		// DBへデータ保存 5
		// 認証成功時に、以下でログイン維持用トークンを更新する）

		// レスポンス値をセット 6
		res.setHeader("Cache-Control", "no-store");

		if (loginFailed) {
			req.setAttribute("loginFailed", true);

		} else {

			// この端末に以前保存したトークンを失効させる
			LoginTokenDao tokenDao = new LoginTokenDao();
			String oldLoginToken = LoginTokenUtil.readToken(req);

			if (oldLoginToken != null) {
				tokenDao.deleteByTokenHash(
						LoginTokenUtil.hashToken(oldLoginToken));
			}

			LoginTokenUtil.clearCookie(req, res);
			session.removeAttribute("loginTokenExpiresAt");
			session.removeAttribute("loginTokenId");
			// 生徒・保護者のみログイン状態を180日間維持する
			if ("STUDENT".equals(user.getRole())
					|| "FAMILY".equals(user.getRole())) {

				String loginToken = LoginTokenUtil.createToken();

				OffsetDateTime expiresAt = OffsetDateTime.now()
						.plusDays(LoginTokenUtil.VALID_DAYS);

				Long loginTokenId = tokenDao.saveAndReturnId(
						user.getId(),
						LoginTokenUtil.hashToken(loginToken),
						expiresAt);

				if (loginTokenId == null) {
					res.sendError(
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}

				// この端末のトークンIDを保持する
				session.setAttribute("loginTokenId", loginTokenId);

				LoginTokenUtil.writeCookie(
						req, res, loginToken, expiresAt);

				// 有効期限の案内表示で使用する
				session.setAttribute(
						"loginTokenExpiresAt", expiresAt);
			}
			// セッション固定攻撃対策
			req.changeSessionId();

			// ログイン済み利用者を保存
			session.setAttribute("user", user);

			// ログイン後のCSRFトークンを再生成
			byte[] tokenBytes = new byte[32];
			new SecureRandom().nextBytes(tokenBytes);

			String newCsrfToken = Base64.getUrlEncoder()
					.withoutPadding()
					.encodeToString(tokenBytes);

			session.setAttribute("csrfToken", newCsrfToken);

			// 無操作時のセッション有効期間：30分
			session.setMaxInactiveInterval(30 * 60);
		}

		// JSPへフォワード 7
		if (loginFailed) {
			req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp")
					.forward(req, res);
		} else {
			res.sendRedirect(req.getContextPath() + redirectPath);
		}
	}
}