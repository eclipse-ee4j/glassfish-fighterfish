/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
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

public class HelloWorldServlet3 extends HttpServlet {

	final static String AttrName = "count";

	public void init(ServletConfig sc) throws ServletException {
		System.out.println(this + ".init(" + sc + ")");
		super.init(sc);
	}

	public void destroy() {
		System.out.println(this + ".destroy()");
	}

	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter writer = resp.getWriter();
		writer.write("<html><body><p>Hello World -- sample servlet #3: </p>");
		ServletContext sc = getServletConfig().getServletContext();
		Integer count = (Integer) sc.getAttribute(AttrName);
		writer.write("<p>servlet context counter = " + count + "</p>");
		HttpSession session = req.getSession(true);
		Integer sessionCount = (Integer)session.getAttribute(AttrName);
		writer.write("<p>http session counter = " + sessionCount + "</p>");
		writer.print("</body></html>");
}

}
