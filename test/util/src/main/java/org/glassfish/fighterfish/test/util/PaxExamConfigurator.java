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
import org.osgi.framework.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.fighterfish.test.util.Constants.FW_CONFIG_FILE_NAME;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * Provides common PAX configuration for all tests.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class PaxExamConfigurator {

    protected static final Logger LOGGER = Logger.getLogger(
            PaxExamConfigurator.class.getPackage().getName());

    private final File gfHome;
    private final long timeout;

    public PaxExamConfigurator(File gfHome, long timeout) {
        this.gfHome = gfHome;
        this.timeout = timeout;
    }

    public Option[] configure() throws IOException {
        return combine(combine(frameworkConfiguration(),
                provisioningBundles()), paxConfiguration());
    }

    private Option[] provisioningBundles() {
        final String version = Version.getVersion();
        LOGGER.logp(Level.INFO, "PaxExamConfigurator", "provisioningBundles",
                "FighterFish Test Util Version = {0}", new Object[]{version});

        final Option gfBundle = bundle(
                new File(gfHome, "modules/glassfish.jar").toURI().toString());
        final Option testUtilBundle;

        File testUtilBundleFile = new File("target/fighterfish-test-util.jar");
        if (testUtilBundleFile.exists()) {
            testUtilBundle = bundle(testUtilBundleFile.toURI().toString());
        } else {
            testUtilBundle = mavenBundle()
                    .groupId("org.glassfish.fighterfish")
                    .artifactId("fighterfish-test-util")
                    .versionAsInProject();
        }
        return options(gfBundle,
                junitBundles(),
                mavenBundle()
                        .groupId("org.apache.httpcomponents")
                        .artifactId("httpclient-osgi")
                        .versionAsInProject(),
                mavenBundle()
                        .groupId("org.apache.httpcomponents")
                        .artifactId("httpcore-osgi")
                        .versionAsInProject(),
                mavenBundle()
                        .groupId("org.slf4j")
                        .artifactId("jcl-over-slf4j")
                        .versionAsInProject(),
                mavenBundle()
                        .groupId("org.slf4j")
                        .artifactId("slf4j-api")
                        .versionAsInProject(),
                mavenBundle()
                        .groupId("org.slf4j")
                        .artifactId("slf4j-jdk14")
                        .versionAsInProject()
                        .noStart(),
                testUtilBundle
        );
    }

    private Option[] frameworkConfiguration() throws IOException {
        // We currently read framework options from a separate file,
        // but we could as well inline them here in code.
        final Properties properties = readFrameworkConfiguration();

        // override by system properties if set in system. We override
        // everything except fileinstall properties as GlassFish's domain.xml
        // is known to set them incorrectly.
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            Object orig = properties.get(entry.getKey());
            Object override = System.getProperty((String) entry.getKey());
            if (override != null) {
                if (String.class.cast(entry.getKey())
                        .startsWith("felix.fileinstall.")) {

                    LOGGER.logp(Level.INFO, "PaxExamConfigurator",
                            "frameworkConfiguration",
                            "Ignoring overriding of {0}", new Object[]{entry});
                    continue;
                }
                properties.put(entry.getKey(), override);
                LOGGER.logp(Level.INFO, "PaxExamConfigurator",
                        "frameworkConfiguration",
                        "entry = {0}, original = {1}, override = {2}",
                        new Object[]{entry.getKey(), orig, override});
            }
        }
        List<Option> options = convertToOptions(properties);
        return options.toArray(new Option[options.size()]);
    }

    private Option[] paxConfiguration() {
        return options(systemTimeout(timeout), cleanCaches(true));
    }

    /**
     * Adapts properties to pax-exam options.
     *
     * @param properties
     * @return
     */
    private List<Option> convertToOptions(Properties properties) {
        List<Option> options = new ArrayList<Option>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (entry.getKey().equals(Constants.FRAMEWORK_STORAGE)) {
                // Starting with pax-exam 2.1.0, we need to specify framework
                // storage using workingDirectory option
                options.add(workingDirectory((String) entry.getValue()));
                LOGGER.logp(Level.INFO, "PaxExamConfigurator",
                        "convertToOptions", "OSGi cache dir = {0}",
                        new Object[]{entry.getValue()});
            }
            options.add(frameworkProperty((String) entry.getKey())
                    .value(entry.getValue()));
        }
        return options;
    }

    private Properties readFrameworkConfiguration() throws IOException {
        Properties properties = new Properties();
        LOGGER.logp(Level.INFO, "DefaultPaxExamConfiguration",
                "readFrameworkConfiguration", "fwConfigFileName = {0}",
                new Object[]{FW_CONFIG_FILE_NAME});
        InputStream stream = getClass().getResourceAsStream(FW_CONFIG_FILE_NAME);
        if (stream != null) {
            try {
                properties.load(stream);
            } finally {
                stream.close();
            }
            PropertiesUtil.substVars(properties);
        } else {
            LOGGER.logp(Level.WARNING, "DefaultPaxExamConfiguration",
                    "readFrameworkConfiguration",
                    "{0} not found. Using default values",
                    new Object[]{FW_CONFIG_FILE_NAME});
        }
        return properties;
    }
}
