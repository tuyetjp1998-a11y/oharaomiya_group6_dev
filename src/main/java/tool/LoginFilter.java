package tool;

import java.io.IOException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.naming.NamingException;

import bean.User;
import dao.LoginTokenDao;
import dao.LoginTokenDao.LoginTokenData;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 生徒・保護者のログイン状態を復元する。
 * 各Actionの認証・権限・CSRF確認はそのまま実行する。
 */
@WebFilter(urlPatterns = { "*.action" })
public class LoginFilter implements Filter {

	@Override
	public void doFilter(
			ServletRequest request,
			ServletResponse response,
			FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		String path = req.getServletPath();

		boolean loginPage = "/safety/auth/Login.action".equals(path);

		boolean targetPage = path.startsWith("/safety/student/")
				|| path.startsWith("/safety/family/")
				|| loginPage;

		// 対象外の画面は通常どおり処理する
		if (!targetPage) {
			chain.doFilter(request, response);
			return;
		}

		res.setHeader("Cache-Control", "no-store");

		// POSTは復元しない。古いフォームのCSRF確認を維持する
		if (!"GET".equalsIgnoreCase(req.getMethod())) {
			chain.doFilter(request, response);
			return;
		}
		// 再ログインの案内から開いた場合は、自動ログインを行わない
		if (loginPage
		        && "true".equals(req.getParameter("reauth"))) {

		    chain.doFilter(request, response);
		    return;
		}
		
		HttpSession session = req.getSession(false);
		
		Object sessionUser = session == null
				? null
				: session.getAttribute("user");

		// ログイン済みの場合はAction側で権限を確認する
		if (sessionUser instanceof User) {
			setLoginExpiryNotice(req, session);

			chain.doFilter(request, response);
			return;
		}

		String currentToken = LoginTokenUtil.readToken(req);

		if (currentToken == null) {
			chain.doFilter(request, response);
			return;
		}

		String newToken = LoginTokenUtil.createToken();
		LoginTokenData tokenData;

		try {
			LoginTokenDao dao = new LoginTokenDao();

			tokenData = dao.rotateAndFindUser(
					LoginTokenUtil.hashToken(currentToken),
					LoginTokenUtil.hashToken(newToken));

		} catch (NamingException | SQLException e) {

			// トークンや接続情報はログに出力しない
			req.getServletContext().log(
					"ログイン状態の復元に失敗しました。例外="
							+ e.getClass().getSimpleName());

			res.sendError(
					HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			return;
		}

		// 無効なトークンではログイン状態を復元しない
		if (tokenData == null) {
			// 同時リクエストで更新されたCookieを消さない
			chain.doFilter(request, response);
			return;
		}

		OffsetDateTime expiresAt = tokenData.getExpiresAt();

		if (!expiresAt.isAfter(OffsetDateTime.now())) {
			chain.doFilter(request, response);
			return;
		}

		// 匿名セッションの情報を引き継がず、新しく作成する
		if (session != null) {
			session.invalidate();
		}

		session = req.getSession(true);

		User user = tokenData.getUser();

		session.setAttribute("user", user);
		// 自動ログイン後も同じトークンIDを保持する
		session.setAttribute(
		        "loginTokenId",
		        tokenData.getTokenId());
		session.setAttribute(
				"csrfToken",
				LoginTokenUtil.createToken());
		session.setAttribute(
				"loginTokenExpiresAt",
				expiresAt);

		// 通常のセッション有効期間は30分のままにする
		session.setMaxInactiveInterval(30 * 60);

		// トークンを更新する。有効期限は延長しない
		LoginTokenUtil.writeCookie(
				req, res, newToken, expiresAt);

		// ログイン画面を開いた場合はホームへ移動する
		if (loginPage) {
			String homePath = "STUDENT".equals(user.getRole())
					? "/safety/student/Home.action"
					: "/safety/family/Home.action";

			res.sendRedirect(
					req.getContextPath() + homePath);
			return;
		}
		setLoginExpiryNotice(req, session);
		chain.doFilter(request, response);
	}

	/**
	 * ログイン維持用トークンの有効期限を画面へ渡す。
	 */
	private void setLoginExpiryNotice(
			HttpServletRequest req,
			HttpSession session) {

		if (session == null) {
			return;
		}

		Object value = session.getAttribute(
				"loginTokenExpiresAt");

		if (!(value instanceof OffsetDateTime)) {
			return;
		}

		OffsetDateTime expiresAt = (OffsetDateTime) value;
		OffsetDateTime now = OffsetDateTime.now();

		// 有効期限の7日前から再ログインを案内する
		boolean renewalRequired = !expiresAt.isAfter(now.plusDays(7));

		req.setAttribute(
				"loginRenewalRequired", renewalRequired);

		req.setAttribute(
				"loginTokenExpired",
				!expiresAt.isAfter(now));

		req.setAttribute(
				"loginExpiresAtText",
				expiresAt.atZoneSameInstant(
						ZoneId.of("Asia/Tokyo"))
						.format(DateTimeFormatter.ofPattern(
								"yyyy/MM/dd")));
	}
}