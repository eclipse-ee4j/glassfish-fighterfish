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

import org.ops4j.pax.exam.Option;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents configuration common to all tests.
 * It reads configuration information from System properties and configures various underlying objects.
 * Depending on configuration, this also installs GlassFish.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class TestsConfiguration {

    private File gfHome;
    private String provisioningUrl;
    /**
     * Timeout corresponding to test execution
     */
    private long testTimeout;

    /**
     * Timeout corresponding to exam system setup
     */
    private long examTimeout;
    private boolean install;
    private File installDir;

    protected Logger logger = Logger.getLogger(getClass().getPackage().getName());

    private static TestsConfiguration instance;

    public synchronized static TestsConfiguration getInstance() {
        if (instance == null) {
            instance = new TestsConfiguration(System.getProperties());
        }
        return instance;
    }

    private TestsConfiguration(Properties properties) {
        testTimeout = Long.parseLong(
                properties.getProperty(Constants.FIGHTERFISH_TEST_TIMEOUT_PROP,
                        Constants.FIGHTERFISH_TEST_TIMEOUT_DEFAULT_VALUE));
        examTimeout = Long.parseLong(
                properties.getProperty(Constants.EXAM_TIMEOUT_PROP,
                        Constants.EXAM_TIMEOUT_DEFAULT_VALUE));
        String property = properties.getProperty(Constants.GLASSFISH_INSTALL_ROOT_PROP);
        if (property != null && !property.isEmpty()) {
            gfHome =  new File(property);
        }
        property = properties.getProperty(Constants.FIGHTERFISH_PROVISIONER_URL_PROP);
        if (property != null && !property.isEmpty()) {
            provisioningUrl = property;
        }
        if (gfHome == null) {
            if (provisioningUrl == null) {
                // both are unspecified
                provisioningUrl = Constants.FIGHTERFISH_PROVISIONER_URL_DEFAULT_VALUE;
            }
            // We compute a hashcode so that if user changes provisioning url, we are less likely to
            // reuse the earlier created installation.
            installDir = new File(System.getProperty("java.io.tmpdir"), "fighterfish-" + provisioningUrl.hashCode());
            gfHome = new File(installDir, "glassfish3/glassfish/");
            install = !installDir.exists();
            if (!install) {
                logger.logp(Level.INFO, "TestsConfiguration", "setup",
                        "Reusing existing installation at {0}", new Object[]{installDir});
            }
        } else {
            // gfHome is specified
            if(!gfHome.exists()) {
                // explode only if provisioning url is explicitly specified
                install = provisioningUrl!= null;
                installDir = new File(gfHome, "../..");
            }
        }
    }

    private void install() {
        if (install) {
            logger.logp(Level.INFO, "TestsConfiguration", "TestsConfiguration",
                "Will install {0} at {1}", new Object[]{provisioningUrl, installDir});
            explode(provisioningUrl, installDir);
        }
        verifyInstallation();
    }

    private void verifyInstallation() {
        final File file = new File(gfHome, "modules/glassfish.jar");
        if (!file.exists()) {
            throw new RuntimeException(file.getAbsolutePath() + " does not exist.");
        }
    }

    private void explode(String provisioningUrl, File out) {
        try {
            ZipUtil.explode(URI.create(provisioningUrl), out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long getTimeout() {
        return testTimeout;
    }

    public long getExamTimeout() {
        return examTimeout;
    }

    private File getGfHome() {
        return gfHome;
    }

    public Option[] getPaxExamConfiguration() throws IOException {
        install();
        return new PaxExamConfigurator(getGfHome(), getExamTimeout()).configure();
    }

    static {
        // Work around for GLASSFISH-16510.
        // This code gets executes before any test methods get executed, which means this code
        // gets executed before any embedded glassfish gets provisioned. By eagely calling, getPlatformMBeanServer,
        // we ensure that all embedded glassfish will use this as opposed to what is created by
        // AppServerMBeanServerBuilder.
        java.lang.management.ManagementFactory.getPlatformMBeanServer();

        // This is needed as we allow user to specify glassfish zip installer using schemes like mvn
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );

    }
}
