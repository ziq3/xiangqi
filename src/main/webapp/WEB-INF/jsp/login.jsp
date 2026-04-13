<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>Login</title>
</head>
<body>
<h2>Login</h2>

<% if (request.getParameter("error") != null) { %>
    <div style="color:red;">Sai tài khoản hoặc mật khẩu</div>
<% } %>

<% if (request.getParameter("logout") != null) { %>
    <div style="color:green;">Đã đăng xuất</div>
<% } %>

<form method="post" action="${pageContext.request.contextPath}/login">
    <div>
        <label>Username:</label>
        <input type="text" name="username" />
    </div>
    <div>
        <label>Password:</label>
        <input type="password" name="password" />
    </div>

    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
    <button type="submit">Login</button>
</form>
</body>
</html>