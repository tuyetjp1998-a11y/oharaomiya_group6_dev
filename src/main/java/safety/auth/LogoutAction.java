package safety.auth;

import bean.User;
import dao.WebPushSubscriptionDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tool.Action;

public class LogoutAction extends Action {

	@Override
	public void execute(HttpServletRequest req,
			HttpServletResponse res) throws Exception {

		// ローカル変数の宣言 1
		HttpSession session = req.getSession(false);
		String expectedToken = null;

		// リクエストパラメーターの取得 2
		String csrfToken = req.getParameter("csrfToken");
		String pushEndpoint = req.getParameter("pushEndpoint");
		// DBからデータ取得 3
		// なし

		// ビジネスロジック 4

		// POST以外のリクエストを拒否
		if (!"POST".equalsIgnoreCase(req.getMethod())) {
			res.setHeader("Allow", "POST");
			res.sendError(
					HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}

		// セッションが既に終了している場合
		if (session == null) {
			res.sendRedirect(req.getContextPath()
					+ "/safety/auth/Login.action");
			return;
		}

		// CSRFトークンを確認
		expectedToken = (String) session.getAttribute("csrfToken");

		if (expectedToken == null
				|| csrfToken == null
				|| !expectedToken.equals(csrfToken)) {

			res.sendError(
					HttpServletResponse.SC_FORBIDDEN,
					"画面を開き直してください。");
			return;
		}

		// ログアウトする保護者の通知登録を削除する
		Object sessionUser = session.getAttribute("user");

		if (sessionUser instanceof User) {
			User user = (User) sessionUser;

			if ("FAMILY".equals(user.getRole())
					&& pushEndpoint != null
					&& !pushEndpoint.isBlank()) {

				if (pushEndpoint.length() > 2048) {
					res.sendError(
							HttpServletResponse.SC_BAD_REQUEST);
					return;
				}

				new WebPushSubscriptionDao()
						.deleteByUserAndEndpoint(
								user.getId(),
								pushEndpoint);
			}
		}
		// セッション内の情報をすべて破棄
		session.invalidate();

		// DBへデータ保存 5
		// なし

		// レスポンス値をセット 6
		res.setHeader("Cache-Control", "no-store");

		// JSPへフォワード 7
		// ログイン画面のActionへリダイレクト
		res.sendRedirect(req.getContextPath()
				+ "/safety/auth/Login.action");
	}
}