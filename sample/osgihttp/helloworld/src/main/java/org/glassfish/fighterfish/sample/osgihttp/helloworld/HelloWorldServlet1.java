/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * This servlet is very similar to {@link HelloWorldServlet2} except that it uses
 * {@link HttpServlet#getServletConfig()#getServletContext()} to retrieve the ServletContext and from there it reads the
 * attribute values to demonstrate that it does not mater how you retrieve the ServletContext, they are all functionally
 * equivalent.
 */
public final class HelloWorldServlet1 extends HttpServlet {

    /**
     * Servlet attribute name.
     */
    static final String ATTRIBUTE_NAME = "count";

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
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("text/html");
        PrintWriter writer = resp.getWriter();
        writer.write("<html><body><p>Hello World -- sample servlet #1: </p>");
        ServletContext sc = getServletConfig().getServletContext();
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
        session.setAttribute(ATTRIBUTE_NAME, ++sessionCount);
        writer.print("</body></html>");
    }
}
