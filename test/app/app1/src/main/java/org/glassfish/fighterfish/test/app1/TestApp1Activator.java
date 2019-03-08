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
package org.glassfish.fighterfish.test.app1;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class TestApp1Activator implements BundleActivator {

    // We should configure this using Config Admin service
    public static final String DS_NAME = "jdbc/__default";
    public static final String TABLE_NAME = "USERINFO";

    private DataSource ds;

    @Override
    public void start(BundleContext context) throws Exception {
        InitialContext ctx = new InitialContext();
        Connection c = null;
        Statement s = null;
        try {
            ds = (DataSource) ctx.lookup(DS_NAME);
            c = ds.getConnection();
            s = c.createStatement();
            String sql = "create table " + TABLE_NAME
                    + " (NAME VARCHAR(10) NOT NULL, PASSWORD VARCHAR(10) NOT NULL,"
                    + " PRIMARY KEY(NAME))";
            System.out.println("sql = " + sql);
            s.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
                if (s != null) {
                    s.close();
                }
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        Connection c = null;
        Statement s = null;
        try {
            c = ds.getConnection();
            s = c.createStatement();
            String sql = "drop table " + TABLE_NAME;
            System.out.println("sql = " + sql);
            s.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
                if (s != null) {
                    s.close();
                }
            } catch (Exception e) {
            }
        }
    }
}
