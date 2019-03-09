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
package org.glassfish.fighterfish.test.util;

/**
 * Constants used in various places.
 */
public final class Constants {

    /**
     * Create a new instance.
     */
    private Constants() {
    }

    /**
     * Name of the properties file containing OSGi framework configuration.
     */
    static final String FW_CONFIG_FILE_NAME = "OSGiFramework.properties";

    /**
     * Property name to specify installation root of GlassFish.
     */
    static final String GLASSFISH_INSTALL_ROOT_PROP
            = "com.sun.aas.installRoot";

    /**
     * Property name to specify installation root URI of GlassFish.
     */
    static final String INSTALL_ROOT_URI_PROP_NAME
            = "com.sun.aas.installRootURI";

    /**
     * Property name used to configure test framework timeout behavior. The
     * value of this property indicates how long will test framework wait before
     * timing out in operations that can possibly never return. When this
     * happens, test framework fails the tests as opposed to hanging for ever or
     * running tests in an incorrect state. One example is when a provisioned
     * bundle never returns from its activator.
     */
    static final String EXAM_TIMEOUT_PROP = "fighterfish.test.setup.timeout";

    /**
     * Property name used to configure timeout values of tests. This depends on
     * tests, but we have a global timeout value for all tests. So, the value
     * must be maximum of each test's timeout value.
     */
    static final String FIGHTERFISH_TEST_TIMEOUT_PROP
            = "fighterfish.test.timeout";

    /**
     * Default timeout value in ms. If no timeout is set using
     * {@link #FIGHTERFISH_TEST_TIMEOUT_PROP}, this value is used.
     */
    static final String FIGHTERFISH_TEST_TIMEOUT_DEFAULT_VALUE = "30000";

    /**
     * Default timeout value in ms. If no timeout is set using
     * {@link #EXAM_TIMEOUT_PROP}, this value is used.
     */
    static final String EXAM_TIMEOUT_DEFAULT_VALUE = "60000"; // in ms

    /**
     * This property is used to specify the directory where derby database will
     * be created. If this property is not specified, then in-memory Derby
     * database will be used during testing.
     */
    public static final String FIGHTERFISH_TEST_DERBY_DB_ROOT_DIR
            = "fighterfish.test.DerbyDBRootDir";

    /**
     * The JNDI name of the default data source configured in the server.
     */
    public static final String DEFAULT_DS = "jdbc/__default";

    /**
     * The JNDI name of the JDBC connection pool used by the default data
     * source.
     */
    public static final String DEFAULT_POOL = "DerbyPool";
}
