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

import org.ops4j.pax.exam.Option;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import static org.glassfish.fighterfish.test.util.Constants.EXAM_TIMEOUT_DEFAULT_VALUE;
import static org.glassfish.fighterfish.test.util.Constants.EXAM_TIMEOUT_PROP;
import static org.glassfish.fighterfish.test.util.Constants.FIGHTERFISH_TEST_TIMEOUT_DEFAULT_VALUE;
import static org.glassfish.fighterfish.test.util.Constants.FIGHTERFISH_TEST_TIMEOUT_PROP;
import static org.glassfish.fighterfish.test.util.Constants.GLASSFISH_INSTALL_ROOT_PROP;
import static org.glassfish.fighterfish.test.util.Constants.INSTALL_ROOT_URI_PROP_NAME;

/**
 * Represents configuration common to all tests. It reads configuration
 * information from System properties and configures various underlying objects.
 * Depending on configuration, this also installs GlassFish.
 */
public final class TestsConfiguration {

    /**
     * Logger.
     */
    protected static final Logger LOGGER = Logger.getLogger(
            TestsConfiguration.class.getPackage().getName());

    /**
     * GlassFish install home.
     */
    private File gfHome;

    /**
     * Timeout corresponding to test execution.
     */
    private final long testTimeout;

    /**
     * Timeout corresponding to exam system setup.
     */
    private final long examTimeout;

    /**
     * Singleton instance.
     */
    private static TestsConfiguration instance;

    /**
     * Get the singleton instance.
     * @return TestsConfiguration
     */
    public static synchronized TestsConfiguration getInstance() {
        if (instance == null) {
            instance = new TestsConfiguration(System.getProperties());
        }
        return instance;
    }

    /**
     * Create a new instance.
     * @param properties config properties
     */
    private TestsConfiguration(final Properties properties) {
        testTimeout = Long.parseLong(
                properties.getProperty(FIGHTERFISH_TEST_TIMEOUT_PROP,
                        FIGHTERFISH_TEST_TIMEOUT_DEFAULT_VALUE));
        examTimeout = Long.parseLong(properties
                .getProperty(EXAM_TIMEOUT_PROP, EXAM_TIMEOUT_DEFAULT_VALUE));
        String installRoot = properties
                .getProperty(GLASSFISH_INSTALL_ROOT_PROP);
        if (installRoot != null && !installRoot.isEmpty()) {
            gfHome = new File(installRoot);
            File glassfishJar = new File(gfHome, "modules/glassfish.jar");
            if (!glassfishJar.exists()) {
                throw new RuntimeException(glassfishJar.getAbsolutePath()
                        + " does not exist.");
            }
            properties.put(INSTALL_ROOT_URI_PROP_NAME,
                    gfHome.toURI().toString());
        } else {
            throw new RuntimeException(GLASSFISH_INSTALL_ROOT_PROP
                    + "property not set");
        }
    }

    /**
     * Get the configured timeout.
     * @return timeout
     */
    public long getTimeout() {
        return testTimeout;
    }

    /**
     * Get the PAX-EXAM timeout.
     * @return timeout
     */
    public long getExamTimeout() {
        return examTimeout;
    }

    /**
     * Get the GlassFish install home.
     * @return File
     */
    private File getGfHome() {
        return gfHome;
    }

    /**
     * Get the PAX-EXAM options.
     * @return Option[]
     * @throws IOException  if an error occurs while reading the config file
     */
    public Option[] getPaxExamConfiguration() throws IOException {
        return new PaxExamConfigurator(gfHome, examTimeout).configure();
    }

    static {
        // Work around for GLASSFISH-16510.
        // This code gets executes before any test methods get executed,
        // which means this code gets executed before any embedded glassfish
        // gets provisioned. By eagely calling, getPlatformMBeanServer,
        // we ensure that all embedded glassfish will use this as opposed to
        // what is created by AppServerMBeanServerBuilder.
        java.lang.management.ManagementFactory.getPlatformMBeanServer();

        // This is needed as we allow user to specify glassfish zip installer
        // using schemes like mvn
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");
    }
}
