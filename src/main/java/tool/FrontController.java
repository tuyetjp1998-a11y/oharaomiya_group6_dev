package tool;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "*.action" })
public class FrontController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse res)
            throws ServletException, IOException {

        // Actionのパスからクラス名を作成
        String path = req.getServletPath();

        String className = path.substring(
                1,
                path.length() - ".action".length())
                .replace('/', '.') + "Action";

        Class<?> actionClass;

        // 対象のActionクラスが存在しない場合は404
        try {
            actionClass = Class.forName(className);

        } catch (ClassNotFoundException e) {
            res.sendError(
                    HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Actionを継承していないクラスは実行しない
        if (!Action.class.isAssignableFrom(actionClass)) {
            res.sendError(
                    HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            Action action = (Action) actionClass
                    .getDeclaredConstructor()
                    .newInstance();

            action.execute(req, res);

        } catch (Exception e) {
            // 詳細はサーバーログに記録する
            getServletContext().log(
                    "Actionの実行中にエラーが発生しました: "
                            + className,
                    e);

            if (res.isCommitted()) {
                throw new ServletException(e);
            }

            // web.xmlのエラーページへ処理を渡す
            res.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res)
            throws ServletException, IOException {

        doGet(req, res);
    }
}