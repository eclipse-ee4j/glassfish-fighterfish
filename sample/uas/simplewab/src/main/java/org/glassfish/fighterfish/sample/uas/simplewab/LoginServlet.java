/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.uas.simplewab;

import java.io.IOException;
import java.io.PrintWriter;

import org.glassfish.fighterfish.sample.uas.api.UserAuthService;
import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.ServiceException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class LoginServlet.
 */
@WebServlet("/LoginServlet")
public final class LoginServlet extends HttpServlet {

    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * User authentication service.
     */
    @Inject
    @OSGiService(dynamic = true)
    private UserAuthService userAuthService;


    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<HTML> <HEAD> <TITLE> Login " + "</TITLE> </HEAD> <BODY BGCOLOR=white>");

        String name = request.getParameter("name");
        String password = request.getParameter("password");
        
        try {
            if (userAuthService.login(name, password)) {
                out.println("Welcome " + name);
            } 
            
            out.println("Incorrect user name or password. Try again");
            
        } catch (ServiceException e) {
            out.println("Service is not yet available");
        }
        
        out.println("</BODY> </HTML> ");
    }
}
