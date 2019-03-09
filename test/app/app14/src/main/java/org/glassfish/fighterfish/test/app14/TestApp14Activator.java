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

package org.glassfish.fighterfish.test.app14;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Bundle activator.
 */
public final class TestApp14Activator implements BundleActivator {

    // We should configure this using Config Admin service
    /**
     * Data source name.
     */
    public static final String DS_NAME = "jdbc/__default";

    @Override
    public void start(final BundleContext context) throws Exception {
        InitialContext ctx = new InitialContext();
        try {
            final DataSource ds = (DataSource) ctx.lookup(DS_NAME);
            System.out.println("TestApp14Activator.start() " + ds);
            ds.getConnection().close(); // just testing
            context.registerService(ConnectionFactory.class.getName(),
                    new ConnectionFactory() {

                @Override
                public Connection getConnection() throws SQLException {
                    return ds.getConnection();
                }
            }, null);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
    }
}
