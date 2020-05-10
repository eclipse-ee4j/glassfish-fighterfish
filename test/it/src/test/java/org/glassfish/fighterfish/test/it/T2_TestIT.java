/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.fighterfish.test.it;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.INFO;
import static org.glassfish.fighterfish.test.util.URLHelper.getResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.fighterfish.test.util.AbstractTestObject;
import org.glassfish.fighterfish.test.util.EjbBundle;
import org.glassfish.fighterfish.test.util.OSGiUtil;
import org.glassfish.fighterfish.test.util.StringPatternMatcher;
import org.glassfish.fighterfish.test.util.TestContext;
import org.glassfish.fighterfish.test.util.URLHelper;
import org.glassfish.fighterfish.test.util.Version;
import org.glassfish.fighterfish.test.util.WebAppBundle;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.http.HttpService;

/**
 * Test scenarios for various integration tests applications of FighterFish project. The test applications can be found
 * in test/testapp directory.
 */
public class T2_TestIT extends AbstractTestObject {

    private static final Logger LOGGER = Logger.getLogger(T2_TestIT.class.getPackage().getName());
    private static final String CLASS_NAME = T2_TestIT.class.getSimpleName();

    /**
     * Tests test.app0.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    public void testapp0() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp0", "ENTRY");
        
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle = testContext.installBundle(testAppLocation("test-app-0", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle);
            wab.deploy(getTimeout(), MILLISECONDS);
            
            String response = wab.getHttpGetResponse("");
            LOGGER.logp(INFO, CLASS_NAME, "testapp0", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher("Hello World"));
        } finally {
            testContext.destroy();
        }
    }

    /**
     * Tests test.app1.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    public void testapp1() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp1", "ENTRY");
        
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle = testContext.installBundle(testAppLocation("test-app-1", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle);
            wab.deploy(getTimeout(), MILLISECONDS);
            
            String response = wab.getHttpPostResponse("/RegistrationServlet?name=foo&password=bar");
            LOGGER.logp(INFO, CLASS_NAME, "testapp1", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher("Registered foo"));
            
            response = wab.getHttpGetResponse("/LoginServlet?name=foo&password=bar");
            LOGGER.logp(INFO, CLASS_NAME, "testapp1", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher("Welcome foo"));
        } finally {
            testContext.destroy();
        }
    }

    /**
     * Tests test.app2.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    public void testapp2() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp2", "ENTRY");
        
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle = testContext.installBundle(testAppLocation("test-app-2", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle);
            wab.deploy(getTimeout(), MILLISECONDS);
            final String registrationRequest = "/RegistrationServlet?name=foo&password=bar";
            final String loginRequest = "/LoginServlet?name=foo&password=bar";
            final String registrationSuccessful = "Registered foo";
            final String loginSuccessful = "Welcome foo";
            String response = wab.getHttpPostResponse(registrationRequest);
            LOGGER.logp(INFO, CLASS_NAME, "testapp1", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher(registrationSuccessful));
            response = wab.getHttpGetResponse(loginRequest);
            LOGGER.logp(INFO, CLASS_NAME, "testapp1", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher(loginSuccessful));
        } finally {
            testContext.destroy();
        }
    }

    /**
     * Tests test.app3.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    public void testapp3() throws GlassFishException, InterruptedException, BundleException, IOException {

        LOGGER.logp(INFO, CLASS_NAME, "testapp3", "ENTRY");
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle = testContext.installBundle(testAppLocation("test-app-3", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle);
            wab.deploy(getTimeout(), MILLISECONDS);
            final String request = "/";
            final String expectedResponse = "Hello from POJO!";
            String response = wab.getHttpGetResponse(request);
            LOGGER.logp(INFO, CLASS_NAME, "testapp3", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher(expectedResponse));
        } finally {
            testContext.destroy();
        }
    }

    /**
     * Tests test.app4.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    public void testapp4() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp4", "ENTRY");
        
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle = testContext.installBundle(testAppLocation("test-app-4", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle);
            wab.deploy(getTimeout(), MILLISECONDS);
            final String request = "/?username=superman";
            final String expectedResponse = "Hello, superman";
            String response = wab.getHttpGetResponse(request);
            LOGGER.logp(INFO, CLASS_NAME, "testapp4", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher(expectedResponse));
        } finally {
            testContext.destroy();
        }
    }

    /**
     * Tests test.app5.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    public void testapp5() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp5", "ENTRY");
        
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle = testContext.installBundle(testAppLocation("test-app-5", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle);
            wab.deploy(getTimeout(), MILLISECONDS);
            final String request = "/";
            final String expectedResponse = "My name is Duke.";
            String response = wab.getHttpGetResponse(request);
            LOGGER.logp(INFO, CLASS_NAME, "testapp5", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher(expectedResponse));
        } finally {
            testContext.destroy();
        }
    }

    /**
     * Tests test.app6.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    @Ignore
    // This is currently failing for EclipseLink's inability to handle URL
    // with bundle scheme.
    public void testapp6() throws GlassFishException, InterruptedException, BundleException, IOException {

        LOGGER.logp(INFO, CLASS_NAME, "testapp6", "ENTRY");
        TestContext testContext = TestContext.create(getClass());
        try {
            empDeptCrud(testContext, testAppLocation("test-app-6", "war"), "testapp6");
        } finally {
            testContext.destroy();
        }
    }

    /**
     * Tests test.app7.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    @Ignore
    // This is currently failing for EclipseLink's inability to handle URL
    // with bundle scheme.
    public void testapp7() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp7", "ENTRY");
        
        TestContext testContext = TestContext.create(getClass());
        try {
            empDeptCrud(testContext, testAppLocation("test-app-7", "war"), "testapp7");
        } finally {
            testContext.destroy();
        }
    }

    /**
     * Tests test.app8.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    public void testapp8() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp8", "ENTRY");
        
        TestContext testContext = TestContext.create(getClass());
        try {
            empDeptCrud(testContext, testAppLocation("test-app-8", "war"), "testapp8");
        } finally {
            testContext.destroy();
        }
    }

    /**
     * Tests test.app9.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    public void testapp9() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp9", "ENTRY");
        
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle = testContext.installBundle(testAppLocation("test-app-9", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle);
            wab.deploy(getTimeout(), MILLISECONDS);
            final String request = "/";
            final String expectedResponse = "Success";
            String response = wab.getHttpGetResponse(request);
            LOGGER.logp(INFO, CLASS_NAME, "testapp9", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher(expectedResponse));
        } finally {
            testContext.destroy();
        }
    }

    /**
     * Tests test.app10.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    @Ignore
    public void testapp10() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp10", "ENTRY");
        
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle = testContext.installBundle(testAppLocation("test-app-10", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle);
            wab.deploy(getTimeout(), MILLISECONDS);
            
            String response = wab.getHttpGetResponse("/");
            LOGGER.logp(INFO, CLASS_NAME, "testapp10", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher("bean: bar"));
        } finally {
            testContext.destroy();
        }
    }

    /**
     * Tests test.app11.ejb.
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    public void testapp11_ejb() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp11_ejb", "ENTRY");
        
        TestContext tc = TestContext.create(getClass());
        try {
            // Tests only deploying an ejb bundle with remote and local ejb in
            // it to make sure the bug reported in #11855 is fixed.
            Bundle bundle_ejb = tc.installBundle(testAppLocation("test-app-11-ejb"));
            EjbBundle ejbBundle = new EjbBundle(ctx, bundle_ejb, new String[] { "org.glassfish.fighterfish.test.app11.ejb.TestLocal" });
            ejbBundle.deploy(getTimeout(), MILLISECONDS);

        } finally {
            tc.destroy();
        }
    }

    /**
     * Tests test.app11 as a WAB
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    @Ignore
    // Currently this does not work because of remote ejb class loading issue
    // yet to be understood and filed as a bug
    public void testapp11_wab() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp11_wab", "ENTRY");
        
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle_ejb = testContext.installBundle(testAppLocation("test-app-11-ejb"));
            EjbBundle ejbBundle = new EjbBundle(ctx, bundle_ejb, new String[] { "org.glassfish.fighterfish.test.app11.ejb.TestLocal" });
            ejbBundle.deploy(getTimeout(), MILLISECONDS);

            // now let's deploy the war as a WAB
            Bundle bundle_web = testContext.installBundle(testAppLocation("test-app-11", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle_web);
            wab.deploy(getTimeout(), MILLISECONDS);
            String request = "/TestServlet";
            String expectedResponse = "HELLO WORLD";
            String response = wab.getHttpGetResponse(request);
            LOGGER.logp(INFO, CLASS_NAME, "testapp11_wab", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher(expectedResponse));
        } finally {
            testContext.destroy();
        }
    }

    /**
     * Tests test.app11 as a plain war
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    @Ignore
    // Currently this does not work because of remote ejb class loading issue
    // yet to be understood and filed as a bug
    public void testapp11_war() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp11_war", "ENTRY");
        
        TestContext testContext = TestContext.create(getClass());
        String appName = null;
        try {
            Bundle bundle_ejb = testContext.installBundle(testAppLocation("test-app-11-ejb"));
            EjbBundle ejbBundle = new EjbBundle(ctx, bundle_ejb, new String[] { "org.glassfish.fighterfish.test.app11.ejb.TestLocal" });
            ejbBundle.deploy(getTimeout(), MILLISECONDS);

            // let's deploy a regular web app
            appName = testContext.getGlassFish().getDeployer().deploy(URI.create(testAppLocation("test-app-11", "war")), "--contextroot", "test.app11");
            final String request = "http://localhost:8080/test.app11/TestServlet";
            final String expectedResponse = "HELLO WORLD";
            String response = getResponse(new URL(request));
            LOGGER.logp(INFO, CLASS_NAME, "testapp11_war", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher(expectedResponse));
        } finally {
            if (appName != null) {
                testContext.getGlassFish().getDeployer().undeploy(appName);
            }
            testContext.destroy();
        }
    }

    /**
     * Tests test.app12
     *
     * @throws org.osgi.framework.BundleException
     * @throws org.glassfish.embeddable.GlassFishException
     * @throws java.lang.InterruptedException
     * @throws java.io.IOException
     */
    @Test
    public void testapp12() throws BundleException, GlassFishException, InterruptedException, IOException {

        LOGGER.logp(INFO, CLASS_NAME, "testapp12", "ENTRY");
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle_host = testContext.installBundle(testAppLocation("test-app-12", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle_host);
            wab.deploy(getTimeout(), MILLISECONDS);

            String requestHost = "/";
            String requestFragment = "/fragment.html";
            String expectedResponseHost = "Hello Host";
            String expectedResponseFragment = "Hello Fragment";
            String response = wab.getHttpGetResponse(requestHost);
            assertThat(response, new StringPatternMatcher(expectedResponseHost));

            // now request the fragment resource
            try {
                wab.getHttpGetResponse(requestFragment);
                fail("Expected fragment to be not available");
            } catch (IOException e) {
                Assert.assertTrue("Expected FileNotFoundException", e instanceof FileNotFoundException);
            }

            // now install the fragment and refresh the host
            testContext.installBundle(testAppLocation("test-app-12-fragment"));
            // This is needed so that the web app does not
            // get deployed upon update().
            bundle_host.stop();
            bundle_host.update();
            // TODO(Sahoo): because of some bug, we can't reuse earlier wab
            wab = new WebAppBundle(ctx, bundle_host);
            wab.deploy(getTimeout(), MILLISECONDS); // deploy again
            response = wab.getHttpGetResponse(requestFragment);
            assertThat(response, new StringPatternMatcher(expectedResponseFragment));

        } finally {
            testContext.destroy();
        }
    }

    @Test
    public void testapp13() throws GlassFishException, InterruptedException, BundleException {

        LOGGER.logp(INFO, CLASS_NAME, "testapp13", "ENTRY");
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle = testContext.installBundle(testAppLocation("test-app-13"));
            EjbBundle ejbBundle = new EjbBundle(ctx, bundle, new String[] { "org.glassfish.fighterfish.test.app13.DummySessionBeanLocal" });
            ejbBundle.deploy(getTimeout(), MILLISECONDS);
            // if deployment has been successful, then the test has passed
        } finally {
            testContext.destroy();
        }
    }

    @Test
    public void testapp14() throws GlassFishException, InterruptedException, BundleException {

        LOGGER.logp(INFO, CLASS_NAME, "testapp14", "ENTRY");
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle = testContext.installBundle(testAppLocation("test-app-14"));
            bundle.start();
            Object service = OSGiUtil.waitForService(ctx, bundle, "org.glassfish.fighterfish.test.app14.ConnectionFactory", getTimeout());
            Assert.assertNotNull(service);
        } finally {
            testContext.destroy();
        }
    }

    @Test
    public void testapp15() throws GlassFishException, InterruptedException, BundleException {

        LOGGER.logp(INFO, CLASS_NAME, "testapp15", "ENTRY");
        TestContext testContext = TestContext.create(getClass());
        try {
            Bundle bundle = testContext.installBundle(testAppLocation("test-app-15"));
            bundle.start();
            Object service = OSGiUtil.waitForService(ctx, bundle, "org.glassfish.fighterfish.test.app15.ConnectionFactory", getTimeout());
            Assert.assertNotNull(service);
        } finally {
            testContext.destroy();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testapp16() throws GlassFishException, InterruptedException, BundleException, IOException {
        LOGGER.logp(INFO, CLASS_NAME, "testapp16", "ENTRY");
        TestContext testContext = TestContext.create(getClass());
        String cfName = "jms/fighterfish.TestApp16ConnectionFactory";
        String topicName = "jms/fighterfish.TestApp16Topic";
        testContext.createJmsCF(cfName);
        testContext.createJmsTopic(topicName);
        
        try {
            String request = "/MessageReaderServlet";
            Bundle bundle_entities = testContext.installBundle(testAppLocation("test-app-16-entities"));
            bundle_entities.start();
            Object service = OSGiUtil.waitForService(ctx, bundle_entities, "jakarta.persistence.EntityManagerFactory", getTimeout());
            Assert.assertNotNull("Checking for EMF svc registered by entities bundle", service);
            Bundle bundle_mdb = testContext.installBundle(testAppLocation("test-app-16-mdb"));
            bundle_mdb.start();
            Bundle bundle_msgproducer = testContext.installBundle(testAppLocation("test-app-16-msgproducer"));
            bundle_msgproducer.start();
            Bundle bundle_wab = testContext.installBundle(testAppLocation("test-app-16", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle_wab);
            
            // Note, bundle deployment happens in the same order as they are
            // started. Since we are not waiting for mdb deployment, let's wait
            // double time for this to deploy.
            wab.deploy(getTimeout() * 2, MILLISECONDS);
            String response = wab.getHttpGetResponse(request);
            assertThat(response, new StringPatternMatcher("Total number of messages: 0"));
            ConfigurationAdmin ca = OSGiUtil.getService(ctx, ConfigurationAdmin.class, getTimeout());
            String pkgName = "org.glassfish.fighterfish.test.app16.msgproducer";
            Configuration config = ca.getConfiguration(pkgName, null);
            Properties props = new Properties();
            props.setProperty(pkgName + ".ConnectionFactory", cfName);
            props.setProperty(pkgName + ".Destination", topicName);
            Integer noOfMsgs = 2;
            props.setProperty(pkgName + ".NoOfMsgs", noOfMsgs.toString());
            config.update((Dictionary) props);
            LOGGER.logp(INFO, CLASS_NAME, "testapp16", "Sleeping for {0} ms for config changes to be propagated and msgs to be delivered",
                    new Object[] { getTimeout() });
            
            // Allow the config changes to be propagated and msg to reach
            // destination
            Thread.sleep(getTimeout());
            LOGGER.logp(INFO, CLASS_NAME, "testapp16", "Waking up from sleep");
            response = wab.getHttpGetResponse(request);
            
            int expectedNoOfMsgs = (noOfMsgs) * 2; // we have 2 MDBs
            LOGGER.logp(INFO, CLASS_NAME, "testapp16", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher("Total number of messages: " + expectedNoOfMsgs));
        } finally {
            testContext.destroy();
        }
    }

    @Test
    public void testapp17() throws GlassFishException, InterruptedException, BundleException, IOException {

        LOGGER.logp(INFO, CLASS_NAME, "testapp17", "ENTRY");
        TestContext tc = TestContext.create(getClass());
        try {
            Bundle bundle_wab = tc.installBundle(testAppLocation("test-app-17", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle_wab);
            // deployment is sufficient to test this bundle
            wab.deploy(getTimeout(), MILLISECONDS);
            String request = "/HelloWebServiceService?wsdl";
            String response = wab.getHttpGetResponse(request);
            LOGGER.logp(INFO, CLASS_NAME, "testapp17", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher("HelloWebServicePort"));
        } finally {
            tc.destroy();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testapp18() throws GlassFishException, InterruptedException, BundleException, IOException {

        LOGGER.logp(INFO, CLASS_NAME, "testapp18", "ENTRY");
        TestContext tc = TestContext.create(getClass());
        try {
            final Bundle bundle_wab = tc.installBundle(testAppLocation("test-app-18", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle_wab);

            final Semaphore eventRaised = new Semaphore(0);
            EventAdmin eventAdmin = OSGiUtil.getService(ctx, EventAdmin.class, getTimeout());
            Assert.assertNotNull("Event Admin Service not available", eventAdmin);
            Properties props = new Properties();
            String[] topics = { "org/glassfish/fighterfist/test/app18" };
            props.put(EventConstants.EVENT_TOPIC, topics);
            ctx.registerService(EventHandler.class.getName(), new EventHandler() {
                @Override
                public void handleEvent(Event event) {
                    LOGGER.logp(INFO, CLASS_NAME, "testapp18", "log message = {0}", new Object[] { event });
                    eventRaised.release();
                }
            }, (Dictionary) props);
            // deployment is sufficient to test this bundle
            wab.deploy(getTimeout(), MILLISECONDS);
            assertTrue("Incorrect no. of events", eventRaised.tryAcquire(1, getTimeout(), MILLISECONDS));
        } finally {
            tc.destroy();
        }
    }

    /**
     * Regression test for GLASSFISH-11748
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws IOException
     */
    @Test
    public void testapp19() throws GlassFishException, InterruptedException, BundleException, IOException {

        LOGGER.logp(INFO, CLASS_NAME, "testapp19", "ENTRY");
        TestContext tc = TestContext.create(getClass());
        try {
            Bundle bundle = tc.installBundle(testAppLocation("test-app-19"));
            LOGGER.logp(INFO, CLASS_NAME, "testapp19", "Starting " + bundle);
            bundle.start();
            assertTrue("Bundle failed to activate", bundle.getState() == Bundle.ACTIVE);
        } finally {
            tc.destroy();
        }
    }

    /**
     * Regression test case for GLASSFISH_18370
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    public void testapp20() throws GlassFishException, InterruptedException, BundleException, IOException {

        LOGGER.logp(INFO, CLASS_NAME, "testapp20", "ENTRY");
        TestContext tc = TestContext.create(getClass());
        try {
            Bundle bundle = tc.installBundle(testAppLocation("test-app-20", "war"));
            WebAppBundle wab = new WebAppBundle(ctx, bundle);
            wab.deploy(getTimeout(), MILLISECONDS);
            final String request = "/";
            final String expectedResponse = "GLASSFISH-18370 has been fixed!";
            String response = wab.getHttpGetResponse(request);
            LOGGER.logp(INFO, CLASS_NAME, "testapp20", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher(expectedResponse));
        } finally {
            tc.destroy();
        }
    }

    @Test
    @Ignore // felix webconsole does not support servlet 4.
    public void test_GLASSFISH_12975() throws GlassFishException, InterruptedException, BundleException, IOException {

        LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_12975", "ENTRY");
        TestContext tc = TestContext.create(getClass());
        Bundle httpServiceBundle = null;
        Bundle bundle2 = null;
        try {
            HttpService httpService = OSGiUtil.getService(ctx, HttpService.class);
            if (httpService == null) {
                for (Bundle b : ctx.getBundles()) {
                    if ("org.glassfish.fighterfish.osgi-http".equals(b.getSymbolicName())) {
                        httpServiceBundle = b;
                        httpServiceBundle.stop(Bundle.STOP_TRANSIENT);
                        httpServiceBundle.start(Bundle.START_TRANSIENT);
                    }
                }
            }
            httpService = OSGiUtil.getService(ctx, HttpService.class, getTimeout());
            assertNotNull(httpService);
            LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_12975", "httpService = {0}", new Object[] { httpService });

            String location = "mvn:org.apache.felix/org.apache.felix.webconsole/4.3.8/jar";
            String location2 = "mvn:org.glassfish.main.osgi-platforms/felix-webconsole-extension/5.1.0/jar";
            Bundle bundle = tc.installBundle(location);
            // See GlASSFISH-20646: We need to install web-console-extension
            // with a location that has AllPermission.
            // Since TestContext does not expose such an API, we have to install
            // it ourselves and remember to uninstall it when the test is done.
            InputStream is2 = null;
            try {
                is2 = new URL(location2).openStream();
                final String installLocation2 = System.getProperty("com.sun.aas.installRootURI") + "modules/autostart/felix-web-console-extension.jar";
                LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_12975", "Installing bundle from {0} at location  {1}",
                        new Object[] { location2, installLocation2 });
                bundle2 = ctx.installBundle(location2, is2);
                LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_12975", "Installed {0}", new Object[] { bundle2 });
            } finally {
                if (is2 != null) {
                    is2.close();
                }
            }
            bundle.start();
            bundle2.start();
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    char[] pass = {};
                    return new PasswordAuthentication("admin", pass);
                }
            });

            String testurl = "http://localhost:8080/osgi/system/console/bundles";
            LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_12975", "testurl = {0}", new Object[] { testurl });
            URL url = new URL(testurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            int responseCode = conn.getResponseCode();
            for (int j = 0; j < 5; j++) {
                conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_12975", "Sleeping for 5 Seconds on testurl = {0}", new Object[] { testurl });
                    Thread.sleep(5000);
                } else {
                    break;
                }
            }
            String responseMessage = conn.getResponseMessage();
            LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_12975", "responsecode = {0} responseMessage = {1}",
                    new Object[] { responseCode, responseMessage });
            assertEquals("Admin Console Not Available", HttpURLConnection.HTTP_OK, responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                stringBuilder.append(inputLine).append("\n");
            }
            in.close();
            LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_12975", "Response Body = {0}", new Object[] { stringBuilder.toString() });
        } finally {
            if (bundle2 != null) {
                bundle2.uninstall();
                LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_12975", "Uninstalled {0}", new Object[] { bundle2 });
            }
            tc.destroy();
            if (httpServiceBundle != null) {
                httpServiceBundle.stop(Bundle.STOP_TRANSIENT);
            }
        }
    }

    @Test
    public void test_GLASSFISH_18159() throws GlassFishException, InterruptedException, BundleException, IOException {

        LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_18159", "ENTRY");
        TestContext tc = TestContext.create(getClass());
        try {
            // Running a regression test for fix created on GLASSFISH-18159
            Bundle bundle = tc.installBundle(testAppLocation("test-app-3", "war"));
            bundle.start();
            bundle.uninstall();
            assertTrue("Uninstallation failed", bundle.getState() == Bundle.UNINSTALLED);
        } finally {
            tc.destroy();
        }
    }

    /**
     * Regression test case for GLASSFISH_19662
     *
     * @throws GlassFishException
     * @throws InterruptedException
     * @throws BundleException
     * @throws java.io.IOException
     */
    @Test
    public void test_GLASSFISH_19662() throws GlassFishException, InterruptedException, BundleException, IOException {

        LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_19662", "ENTRY");
        TestContext tc = TestContext.create(getClass());
        try {
            // firstly, we install sample.uas.api bundle
            String location = sampleAppLocation("uas-api");
            tc.installBundle(location);

            GlassFish glassfish = tc.getGlassFish();
            Deployer deployer = glassfish.getDeployer();
            // secondly, we install sample.uas.simplewabfragment bundle
            location = sampleAppLocation("uas-simplewabfragment");
            String wabfragmentName = deployer.deploy(URI.create(location), "--type=osgi");

            // finally, we install host bundle called sample.uas.simplewab wab
            location = sampleAppLocation("uas-simplewab", "war");
            String wabName = deployer.deploy(URI.create(location), "--type=osgi");

            Thread.sleep(getTimeout());

            // here, for simplicity, I have not installed UserAuthService
            final String reportJspRequest = "http://localhost:8080/uas/report.jsp";
            final String reportServletRequest = "http://localhost:8080/uas/ReportServlet";
            final String reportJspSuccessful = "Please click";
            final String reportServletSuccessful = "Service is not yet available";

            String response = URLHelper.getResponse(new URL(reportJspRequest));
            LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_19662", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher(reportJspSuccessful));

            response = URLHelper.getResponse(new URL(reportServletRequest));
            LOGGER.logp(INFO, CLASS_NAME, "test_GLASSFISH_19662", "response = {0}", new Object[] { response });
            assertThat(response, new StringPatternMatcher(reportServletSuccessful));

            deployer.undeploy(wabfragmentName);
            deployer.undeploy(wabName);

        } finally {
            tc.destroy();
        }
    }

    private static String sampleAppLocation(String name) {
        return sampleAppLocation(name, null);
    }

    private static String sampleAppLocation(String name, String packaging) {
        String location = "mvn:org.glassfish.fighterfish/fighterfish-sample-" + name + "/" + Version.getVersion();
        if (packaging != null) {
            location += "/" + packaging;
        }
        return location;
    }

    private static String testAppLocation(String name) {
        return testAppLocation(name, null);
    }

    private static String testAppLocation(String name, String packaging) {
        String location = "mvn:org.glassfish.fighterfish/fighterfish-" + name + "/" + Version.getVersion();
        if (packaging != null) {
            location += "/" + packaging;
        }
        return location;
    }

    //////////////////////////////////////////////////////////////////
    // Various utility methods used from test methods are found below.
    //////////////////////////////////////////////////////////////////
    private void empDeptCrud(TestContext tc, String location, String testMethodName) throws BundleException, InterruptedException, IOException {

        Bundle bundle = tc.installBundle(location);
        WebAppBundle wab = new WebAppBundle(ctx, bundle);
        wab.deploy(getTimeout(), MILLISECONDS);
        final String request1 = "/crud?action=createDepartment&departmentName=hr";
        final String request2 = "/crud?action=createDepartment&departmentName=finance";
        final String request3 = "/crud?action=createEmployee&departmentName=finance";
        final String request4 = "/crud?action=createEmployee&departmentName=hr";
        final String request5 = "/crud?action=readEmployee&employeeId=1";
        final String request6 = "/crud?action=readDepartment&departmentName=hr";
        final String request7 = "/crud?action=deleteEmployee&employeeId=2";
        final String request8 = "/crud?action=deleteEmployee&employeeId=1";
        final String request9 = "/crud?action=deleteDepartment&departmentName=hr";
        final String request10 = "/crud?action=deleteDepartment&departmentName=finance";
        final String createdResponse = "Created ";
        final String readResponse = "Found ";
        final String deletedResponse = "Deleted ";
        String response = wab.getHttpPostResponse(request1);
        LOGGER.logp(INFO, CLASS_NAME, testMethodName, "response = {0}", new Object[] { response });
        assertThat(response, new StringPatternMatcher(createdResponse));

        response = wab.getHttpPostResponse(request2);
        LOGGER.logp(INFO, CLASS_NAME, testMethodName, "response = {0}", new Object[] { response });
        assertThat(response, new StringPatternMatcher(createdResponse));

        response = wab.getHttpPostResponse(request3);
        LOGGER.logp(INFO, CLASS_NAME, testMethodName, "response = {0}", new Object[] { response });
        assertThat(response, new StringPatternMatcher(createdResponse));

        response = wab.getHttpPostResponse(request4);
        LOGGER.logp(INFO, CLASS_NAME, testMethodName, "response = {0}", new Object[] { response });
        assertThat(response, new StringPatternMatcher(createdResponse));

        response = wab.getHttpGetResponse(request5);
        LOGGER.logp(INFO, CLASS_NAME, testMethodName, "response = {0}", new Object[] { response });
        assertThat(response, new StringPatternMatcher(readResponse));

        response = wab.getHttpGetResponse(request6);
        LOGGER.logp(INFO, CLASS_NAME, testMethodName, "response = {0}", new Object[] { response });
        assertThat(response, new StringPatternMatcher(readResponse));

        response = wab.getHttpGetResponse(request6);
        LOGGER.logp(INFO, CLASS_NAME, testMethodName, "response = {0}", new Object[] { response });
        assertThat(response, new StringPatternMatcher(readResponse));

        response = wab.getHttpPostResponse(request7);
        LOGGER.logp(INFO, CLASS_NAME, testMethodName, "response = {0}", new Object[] { response });
        assertThat(response, new StringPatternMatcher(deletedResponse));

        response = wab.getHttpPostResponse(request8);
        LOGGER.logp(INFO, CLASS_NAME, testMethodName, "response = {0}", new Object[] { response });
        assertThat(response, new StringPatternMatcher(deletedResponse));

        response = wab.getHttpPostResponse(request9);
        LOGGER.logp(INFO, CLASS_NAME, testMethodName, "response = {0}", new Object[] { response });
        assertThat(response, new StringPatternMatcher(deletedResponse));

        response = wab.getHttpPostResponse(request10);
        LOGGER.logp(INFO, CLASS_NAME, testMethodName, "response = {0}", new Object[] { response });
        assertThat(response, new StringPatternMatcher(deletedResponse));
    }
}
