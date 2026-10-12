<%-- ログイン画面へリダイレクト --%>
<%@ page contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%
    response.sendRedirect(
        request.getContextPath() + "/safety/auth/Login.action"
    );
%>