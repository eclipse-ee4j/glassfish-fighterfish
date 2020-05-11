/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates. All rights reserved.
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

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class MessageReaderServlet.
 */
@WebServlet("/MessageReaderServlet")
public final class MessageReaderServlet extends HttpServlet {

    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Entity manager factory.
     */
    @Inject
    @OSGiService(dynamic = true,
            serviceCriteria = "(persistence-unit=test.app16.entities)")
    private EntityManagerFactory emf;

    @Override
    protected void service(final HttpServletRequest arg0,
            final HttpServletResponse arg1)
            throws ServletException, IOException {

        PrintWriter out = arg1.getWriter();
        out.print("<HTML> <HEAD> <TITLE> MessageReaderServlet"
                + "</TITLE> </HEAD> <BODY BGCOLOR=white>");
        out.print("<p/>");
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Message> cq = cb.createQuery(Message.class);
            Root<Message> root = cq.from(Message.class);
            cq.select(root);
            TypedQuery<Message> q = em.createQuery(cq);
            List<Message> resultList = q.getResultList();
            out.print("Total number of messages: " + resultList.size()
                    + "<p/>");
            for (Message msg : resultList) {
                System.out.println(getClass().getSimpleName() + ": "
                        + msg.getValue() + "\n");
                out.print(msg.getValue() + "<p/>");
            }
        } finally {
            em.close();
        }
        out.println("</BODY> </HTML> ");
    }
}
