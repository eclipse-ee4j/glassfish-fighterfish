/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.fighterfish.test.app2;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;

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
    @EJB
    private UserAuthServiceEJB userAuthService;

    @Override
    public void service(final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, java.io.IOException {

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<HTML> <HEAD> <TITLE> Login "
                + "</TITLE> </HEAD> <BODY BGCOLOR=white>");

        String name = req.getParameter("name");
        String password = req.getParameter("password");
        try {

            if (userAuthService.login(name, password)) {
                out.println("Welcome " + name);
            } else {
                out.println("Incorrect user name or password. Try again");
            }
        } catch (Exception e) {
            out.println(e.getMessage());
            e.printStackTrace();
        }
        out.println("</BODY> </HTML> ");
    }
}
