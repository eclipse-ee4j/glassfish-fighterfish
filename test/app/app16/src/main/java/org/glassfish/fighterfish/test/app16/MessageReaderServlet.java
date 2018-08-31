/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.fighterfish.test.app16;

import org.glassfish.fighterfish.test.app16.entities.Message;
import org.glassfish.osgicdi.OSGiService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class MessageReaderServlet
 */
@WebServlet("/MessageReaderServlet")
public class MessageReaderServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Inject
    @OSGiService(dynamic = true, serviceCriteria = "(persistence-unit=test.app16.entities)")
    private EntityManagerFactory emf;
	
    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void service(HttpServletRequest arg0, HttpServletResponse arg1)
            throws ServletException, IOException {
        PrintWriter out = arg1.getWriter();
        out.print("<HTML> <HEAD> <TITLE> MessageReaderServlet" +
                "</TITLE> </HEAD> <BODY BGCOLOR=white>");
        out.print("<p/>");
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Message> cq = cb.createQuery(Message.class);
            Root<Message> root = cq.from(Message.class);
            cq.select(root);
            TypedQuery<Message> q = em.createQuery(cq);
            List<Message> resultList = q.getResultList();
            out.print("Total number of messages: " + resultList.size() + "<p/>");
            for (Message msg : resultList) {
                System.out.println(getClass().getSimpleName() + ": " + msg.getValue() + "\n");
                out.print(msg.getValue() + "<p/>");
            }
        } finally {
            em.close();
        }
        out.println("</BODY> </HTML> ");
    }
       

}
