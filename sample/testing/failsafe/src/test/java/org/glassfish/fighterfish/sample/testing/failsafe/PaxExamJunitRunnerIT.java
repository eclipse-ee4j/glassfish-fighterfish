/*
 * Copyright (c) 1997-2019 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glassfish.fighterfish.sample.testing.failsafe;

import org.glassfish.embeddable.GlassFishException;
import org.glassfish.fighterfish.test.util.StringPatternMatcher;
import org.glassfish.fighterfish.test.util.TestContext;
import org.glassfish.fighterfish.test.util.TestsConfiguration;
import org.glassfish.fighterfish.test.util.WebAppBundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import javax.inject.Inject;
import java.io.IOException;
import org.glassfish.fighterfish.test.util.Version;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * This test sample demonstrates use of pax-exam provided
 * {@link JUnit4TestRunner} JUnit test runner. This test class name is suffixed
 * with IT to automatically include it in the test list by maven failsafe
 * plugin.
 *
 * This custom runner from PAX-EXAM has the ability to provision an OSGi runtime
 * either in the current JVM or in a remote JVM with a list of bundles
 * configured by the test. The custom runner calls a method in the test class
 * annotated with {@link Configuration} and uses its return value to configure
 * the environment. In our test case, {@link #getPaxExamConfiguration()} is that
 * method.
 *
 * This test case also demonstrates use {@link TestContext} to deploy various
 * artifacts like OSGi bundles, Java EE applications, JDBC/JMS resources. See
 * {@link #test}.
 *
 * @see TestContext
 * @see TestsConfiguration
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class PaxExamJunitRunnerIT {

    /**
     * This is how one can inject BundleContext. In fact, one can even inject
     * provisioned services.
     */
    @Inject
    private BundleContext ctx;

    /**
     * PaxExamJunit driver treats methods in Junit Test class annotated with
     *
     * @Configuration specially. For each such method, it creates a separate
     * test container configuring it with the options as returned by the method.
     *
     * This method implementation calls a utility called
     * {@link TestsConfiguration} provided by FighterFish project to find out
     * what configuration is needed to provision a GlassFish runtime. Although
     * it is really straight forward to configure Pax-Exam to provision a
     * GlassFish runtime, we sugest use of the utility which allows you to just
     * configure the test environment by setting various system properties in
     * pom.xml.
     *
     * @return Options used to configure a test container
     * @throws IOException
     */
    @Configuration
    public Option[] getPaxExamConfiguration() throws IOException {
        // Typical configuration involved in provisioning a GlassFish runtime
        // can be obtained by calling this utility method.
        return TestsConfiguration.getInstance().getPaxExamConfiguration();
    }

    /**
     * A simple test case that deploys a couple of bundles one of which is an
     * API bundle which is consumed by the second one which is a WAB. It then
     * requests a resource from the WAB and compares the output with an expected
     * output.
     *
     * The test uses mvn url scheme to reference the source location of bundles
     * to be deployed. You must have the maven artifacts available in your local
     * or remote maven repo.
     *
     * The test will automatically provision a GlassFish runtime for you.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws IOException
     */
    @Test
    public void test() throws GlassFishException, InterruptedException,
            BundleException, IOException {

        assertNotNull("bundle context is null", ctx);
        TestContext tc = TestContext.create(getClass());
        try {
            // Let's install a couple of bundles one of which is an API bundle 
            // which is consumed by the second one which is a WAB. 
            Bundle uas_api_b = tc.installBundle(
                    sampleAppLocation("uas-api"));
            Bundle uas_simplewab_b = tc.installBundle(
                    sampleAppLocation("uas-simplewab", "war"));
            WebAppBundle uas_simple_webapp = tc.deployWebAppBundle(
                    uas_simplewab_b);
            String response = uas_simple_webapp
                    .getHttpGetResponse("/LoginServlet?name=foo&password=bar");
            System.out.println(response);
            assertThat(response, new StringPatternMatcher(
                    "Service is not yet available"));
        } finally {
            tc.destroy();
        }
    }

    private static String sampleAppLocation(String name) {
        return sampleAppLocation(name, null);
    }

    private static String sampleAppLocation(String name, String packaging) {
        String location = "mvn:org.glassfish.fighterfish/fighterfish-sample-"
                + name + "/" + Version.getVersion();
        if (packaging != null) {
            location += "/" + packaging;
        }
        return location;
    }
}
