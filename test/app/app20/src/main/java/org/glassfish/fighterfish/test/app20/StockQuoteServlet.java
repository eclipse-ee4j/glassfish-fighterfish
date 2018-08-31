/*
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.fighterfish.test.app20;

import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.glassfish.osgicdi.OSGiService;

/**
 * A simple Servlet to test GLASSFISH-18370
 * 
 * @author Tang Yong
 */
@WebServlet(urlPatterns = "/")
public class StockQuoteServlet extends HttpServlet {
	// Inject the OSGi service by specifying the OSGiService qualifier
	@Inject
	@OSGiService
	StockQuoteService sqs;

	public void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, java.io.IOException {
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();

		if (sqs != null) {
			// get the list of symbols and print their current quotes
			out.println("Stock Symbol Service is available!");
			try{
			   sqs.getNullSymbols();
			}catch(NullPointerException ex){
				out.println("GLASSFISH-18370 has been fixed!");
			}
		} else {
			out.println("Stock Symbol Service is not yet available");
		}
	}
}
