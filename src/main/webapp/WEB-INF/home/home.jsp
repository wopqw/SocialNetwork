<%--
  Created by IntelliJ IDEA.
  User: wopqw
  Date: 31.10.16
  Time: 2:19
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="model.UserRole" %>
<%@ page import="java.util.HashSet" %>
<jsp:useBean id="user" type="model.User" scope="request"/>
<jsp:useBean id="userRoles" class="java.util.HashSet" scope="request"/>
<html>
<head>
    <title>HomePage</title>
    <%--<% HashSet<User> roles = (HashSet<String>)session.getAttribute("ROLE");%>--%>
    <script type="text/javascript" src="<c:url value='../../resources/js/jquery-3.1.0.min.js'/>"></script>
    <script type="text/javascript" src="<c:url value="../../resources/js/userScript.js"/>"></script>
</head>
<body>
Welcome to your homePage, <%=user.getFirstName()%>!
<br>
Your role in this project: <br>
<% HashSet<UserRole> roles = (HashSet<UserRole>) userRoles;%>
<% for(UserRole role: roles){%>
<br><%=role%>
<%}%>
<br>
<p>timeline:</p>
<div id="timeline"></div>
</body>
<script>
    var timeline = new Timeline(userId = '${user.id}',
            postContainer = document.getElementById('timeline'));
    addEventListener("DOMContentLoaded", timeline.loadTimeline, false)
</script>
</html>
