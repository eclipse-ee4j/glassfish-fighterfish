/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.fighterfish.test.app7;

import javax.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.UserTransaction;

import java.io.PrintWriter;

/**
 * Servlet implementation class CRUDServlet.
 */
@WebServlet("/crud")
public final class CRUDServlet extends HttpServlet {

    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = 9133422911516243972L;

    /**
     * User transaction.
     */
    @Resource
    private UserTransaction utx;

    @Override
    public void service(final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, java.io.IOException {

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.print("<HTML> <HEAD> <TITLE> Employee-Department CRUD"
                + "</TITLE> </HEAD> <BODY BGCOLOR=white>");
        out.print("\n");

        try {
            EntityManagerFactory emf = Persistence
                    .createEntityManagerFactory("em1");
            utx.begin();
            EntityManager em = emf.createEntityManager();
            out.print(em);
            out.print("\n");
            String action = (String) req.getParameter("action");
            out.print("action = " + action);
            out.print("\n");
            if ("createEmployee".equals(action)) {
                String departmentName = (String) req
                        .getParameter("departmentName");
                Department d = em.find(Department.class, departmentName);
                Employee e = new Employee();
                e.setDepartment(d);
                em.persist(e);
                out.print("Created " + e);
                out.print("\n");
            } else if ("readEmployee".equals(action)) {
                String employeeId = (String) req.getParameter("employeeId");
                Employee e = em.find(Employee.class,
                        Integer.parseInt(employeeId));
                out.print("Found " + e);
                out.print("\n");
            } else if ("deleteEmployee".equals(action)) {
                String employeeId = (String) req.getParameter("employeeId");
                Employee e = em.find(Employee.class,
                        Integer.parseInt(employeeId));
                if (e != null) {
                    em.remove(e);
                }
                out.print("Deleted " + e);
                out.print("\n");
            } else if ("createDepartment".equals(action)) {
                String name = (String) req.getParameter("departmentName");
                Department d = new Department(name);
                em.persist(d);
                out.print("Created " + d);
                out.print("\n");
            } else if ("readDepartment".equals(action)) {
                String name = (String) req.getParameter("departmentName");
                Department d = em.find(Department.class, name);
                out.print("Found " + d);
                out.print("\n");
            } else if ("deleteDepartment".equals(action)) {
                String name = (String) req.getParameter("departmentName");
                Department d = em.find(Department.class, name);
                if (d != null) {
                    em.remove(d);
                }
                out.print("Deleted " + d);
                out.print("\n");
            }
            utx.commit();
            emf.close();
        } catch (Exception e) {
            e.printStackTrace(out);
        }
        out.println("</BODY> </HTML> ");
    }
}
