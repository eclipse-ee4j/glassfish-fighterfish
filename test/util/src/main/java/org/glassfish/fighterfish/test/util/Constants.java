/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
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
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public final class Constants {
    /**
     * Name of the properties file containing OSGi framework configuration
     */
    static final String FW_CONFIG_FILE_NAME = "OSGiFramework.properties";

    /**
     * Name of property used to indicate which platform is being used to run GlassFish. Values are Felix or Equinox
     */
    static final String GLASSFISH_PLATFORM_PROP = "GlassFish_Platform";

    /**
     * Default value for {@link #GLASSFISH_PLATFORM_PROP}
     */
    static final String DEFAULT_GLASSFISH_PLATFORM = "Felix";

    /**
     * Property name to specify installation root of GlassFish.
     */
    static final String GLASSFISH_INSTALL_ROOT_PROP = "com.sun.aas.installRoot";

    /**
     * Property name used to configure test framework timeout behavior. The value of this property indicates how long will
     * test framework wait before timing out in operations that can possibly never return. When this happens,
     * test framework fails the tests as opposed to hanging for ever or running tests in an incorrect state.
     * One example is when a provisioned bundle never returns from its activator.
     */
    static final String EXAM_TIMEOUT_PROP = "fighterfish.test.setup.timeout";

    /**
     * Property name used to configure timeout values of tests. This depends on tests, but we have a global timeout
     * value for all tests. So, the value must be maximum of each test's timeout value.
     */
    static final String FIGHTERFISH_TEST_TIMEOUT_PROP = "fighterfish.test.timeout";

    /**
     * Default timeout value in ms. If no timeout is set using {@link #FIGHTERFISH_TEST_TIMEOUT_PROP}, this value is used.
     */
    static final String FIGHTERFISH_TEST_TIMEOUT_DEFAULT_VALUE = "30000"; // in ms

    /**
     * Default timeout value in ms. If no timeout is set using {@link #EXAM_TIMEOUT_PROP}, this value is used.
     */
    static final String EXAM_TIMEOUT_DEFAULT_VALUE = "60000"; // in ms

    /**
     * URL string used to download glassfish distribution. e.g.:
     * mvn:org.glassfish.distributions/glassfish/3.1.1/zip
     * file:/tmp/web.zip
     * http://maven.glassfish.org/content/groups/glassfish/org/glassfish/distributions/nucleus/3.1.1/nucleus-3.1.1.zip
     *
     */
    public static final String FIGHTERFISH_PROVISIONER_URL_PROP = "fighterfish.provisioner.url";
    public static final String FIGHTERFISH_PROVISIONER_URL_DEFAULT_VALUE = "mvn:org.glassfish.distributions/glassfish/3.1.1/zip";

    /**
     * This property is used to specify the directory where derby database will be created.
     * If this property is not specified, then in-mempry Derby database will be used during testing.
     */
    public static final String FIGHTERFISH_TEST_DERBY_DB_ROOT_DIR = "fighterfish.test.DerbyDBRootDir";

    /**
     * The JNDI name of the default datasource configured in the server.
     */
    public static final String DEFAULT_DS = "jdbc/__default";

    /**
     * The JNDI name of the JDBC connection pool used by the default datasource
     */
    public static final String DEFAULT_POOL = "DerbyPool";
}
