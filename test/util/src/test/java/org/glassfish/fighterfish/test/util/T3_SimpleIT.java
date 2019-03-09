/*
 * Copyright (c) 2012, 2019 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.embeddable.GlassFishException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import javax.inject.Inject;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Test3.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class T3_SimpleIT {

    /**
     * Pax-Exam supports injection.
     */
    @Inject
    private BundleContext ctx;

    /**
     * PaxExamJunit driver treats methods in JUnit Test class annotated with
     *
     * @Configuration specially. For each such method, it creates a separate
     * test container configuring it with the options as returned by the method.
     *
     * @return Options used to configure a test container
     * @throws IOException
     */
    @Configuration
    public Option[] getPaxExamConfiguration() throws IOException {
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
    public void test()throws GlassFishException, InterruptedException,
            BundleException, IOException {

        assertNotNull(ctx);
        TestContext tc = TestContext.create(getClass());
        try {
            // Let's install a couple of bundles one of which is an API bundle
            // which is consumed by the second one which is a WAB.
            Bundle uas_api_b = tc.installBundle(
                    "mvn:org.glassfish.fighterfish/fighterfish-sample-uas-api/"
                            + Version.getVersion());
            Bundle uas_simplewab_b = tc.installBundle(
                    "mvn:org.glassfish.fighterfish/fighterfish-sample-uas-simplewab/"
                            + Version.getVersion() + "/war");
            WebAppBundle uas_simple_webapp = tc.deployWebAppBundle(uas_simplewab_b);
            String response = uas_simple_webapp.getHttpGetResponse(
                    "/LoginServlet?name=foo&password=bar");
            System.out.println(response);
            assertThat(response, new StringPatternMatcher(
                    "Service is not yet available"));
        } finally {
            tc.destroy();
        }
    }
}
