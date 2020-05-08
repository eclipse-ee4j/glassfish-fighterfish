/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgijdbc;

/**
 * Constants used by this module.
 */
public final class Constants {

    /**
     * Cannot be instanciated.
     */
    private Constants() {
    }

    /**
     * Property name for JNDI name property.
     */
    public static final String JNDI_NAME = "jndi-name";

    /**
     * Property name for {@code javax.sql.DataSource}.
     */
    public static final String DS = "javax.sql.DataSource";

    /**
     * Property name for {@code javax.sql.ConnectionPoolDataSource}.
     */
    public static final String CPDS = "javax.sql.ConnectionPoolDataSource";

    /**
     * Property name for {@code javax.sql.XADataSource}.
     */
    public static final String XADS = "javax.sql.XADataSource";

    /**
     * Property name for {@code java.sql.Driver}.
     */
    public static final String DRIVER = "java.sql.Driver";

    /**
     * Constant for db vendor.
     */
    public static final String DBVENDOR = "dbvendor";

    /**
     * Constant for JDBC driver URI scheme.
     */
    public static final String JDBC_DRIVER_SCHEME = "jdbcdriver";

    /**
     * Constant for {@code OSGI_RFC_122}.
     */
    public static final String OSGI_RFC_122 = "OSGI_RFC_122";

    /**
     * Constant for {@Code Implementation-Version} manifest entry.
     */
    public static final String IMPL_VERSION = "Implementation-Version";

    /**
     * Constant for {@Code Implementation-Vendor} manifest entry.
     */
    public static final String IMPL_VENDOR = "Implementation-Vendor";

    /**
     * Constant for {@Code Implementation-Title} manifest entry.
     */
    public static final String IMPL_TITLE = "Implementation-Title";
}
