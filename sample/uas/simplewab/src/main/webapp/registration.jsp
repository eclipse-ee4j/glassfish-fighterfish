<%--

    Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Distribution License v. 1.0, which is available at
    http://www.eclipse.org/org/documents/edl-v10.php.

    SPDX-License-Identifier: BSD-3-Clause

--%>

<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <title>New User Registation Page</title>
    </head>

    <body>
        <hr>
        <form method="post" action="RegistrationServlet">
            <p>User Name: <input type="text" name="name" size="10"> </p>
            <p>Password: <input type="password" name="password" size="10"> </p>
            <br>
            <p>
                <input type="submit" value="register"/>
                <input type="reset" value="cancel"/>
        </form>
    </body>
</html>
