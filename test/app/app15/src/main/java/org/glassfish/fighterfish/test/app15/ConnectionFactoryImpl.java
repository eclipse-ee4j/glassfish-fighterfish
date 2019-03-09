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

package org.glassfish.fighterfish.test.app15;

import org.osgi.service.component.ComponentContext;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Implementation of {@link ConnectionFactory}.
 */
public final class ConnectionFactoryImpl implements ConnectionFactory {

    /**
     * Data source name.
     */
    // should use Config Admin to get this
    private static final String DSNAME = "jdbc/__default";

    /**
     * Data source.
     */
    private DataSource ds;

    @Override
    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    /**
     * Activate the data source.
     * @param compCtx component context
     */
    public void activate(final ComponentContext compCtx) {
        System.out.println("ConnectionFactoryImpl.activate()");
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource) ctx.lookup(DSNAME);
            System.out.println("ConnectionFactoryImpl.activate() " + ds);
            ds.getConnection().close(); // just testing
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deactivate the data source.
     * @param compCtx component context
     */
    public void deactivate(final ComponentContext compCtx) {
        System.out.println("ConnectionFactoryImpl.deactivate()");
    }
}
