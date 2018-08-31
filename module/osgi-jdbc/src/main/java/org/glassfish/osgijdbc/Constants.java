/*
 * Copyright (c) 2009, 2018 Oracle and/or its affiliates. All rights reserved.
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
 * Constants that will be used by osgi-jdbc module
 *
 * @author Jagadish Ramu
 */
public interface Constants {

    String JNDI_NAME = "jndi-name";

    String DS = "javax.sql.DataSource";
    String CPDS = "javax.sql.ConnectionPoolDataSource";
    String XADS = "javax.sql.XADataSource";
    String DRIVER = "java.sql.Driver";
    String DBVENDOR = "dbvendor";

    String JDBC_DRIVER_SCHEME = "jdbcdriver";
    String OSGI_RFC_122 = "OSGI_RFC_122";
    String IMPL_VERSION = "Implementation-Version";
    String IMPL_VENDOR = "Implementation-Vendor";
    String IMPL_TITLE = "Implementation-Title";
}
