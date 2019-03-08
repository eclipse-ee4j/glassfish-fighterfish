/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.osgihttp.helloworld;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This servlet is very similar to {@link HelloWorldServlet1} except that it
 * uses {@link HttpServletRequest#getSession(boolean)#getServletContext()} to
 * retrieve the ServletContext and from there it reads the attribute values to
 * demonstrate that it does not mater how you retrieve the ServletContext, they
 * are all functionally equivalent.
 */
public final class HelloWorldServlet2 extends HttpServlet {

    /**
     * Servlet attribute name.
     */
    private static final String ATTRIBUTE_NAME = "count";

    @Override
    public void init(final ServletConfig sc) throws ServletException {
        System.out.println(this + ".init(" + sc + ")");
        super.init(sc);
    }

    @Override
    public void destroy() {
        System.out.println(this + ".destroy()");
    }

    @Override
    protected void service(final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html");
        PrintWriter writer = resp.getWriter();
        writer.write("<html><body><p>Hello World -- sample servlet #2: </p>");
        ServletContext sc = req.getSession(true).getServletContext();
        Integer count = (Integer) sc.getAttribute(ATTRIBUTE_NAME);
        writer.write("<p>servlet context counter = " + count + "</p>");
        if (count == null) {
            count = 0;
        }
        sc.setAttribute(ATTRIBUTE_NAME, ++count);

        HttpSession session = req.getSession(true);
        Integer sessionCount = (Integer) session.getAttribute(ATTRIBUTE_NAME);
        writer.write("<p>http session counter = " + sessionCount + "</p>");
        if (sessionCount == null) {
            sessionCount = 0;
        }
        session.setAttribute(ATTRIBUTE_NAME, sessionCount + 1);
        writer.print("</body></html>");
    }
}
