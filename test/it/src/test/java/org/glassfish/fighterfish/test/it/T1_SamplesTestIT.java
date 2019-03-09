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
package org.glassfish.fighterfish.test.it;

import org.glassfish.embeddable.GlassFishException;
import org.glassfish.fighterfish.test.util.*;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.http.HttpService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.fighterfish.test.util.URLHelper.getResponse;
import static org.junit.Assert.*;
import static org.osgi.framework.Bundle.START_TRANSIENT;

/**
 * Test scenarios for various FighterFish samples.
 */
public class T1_SamplesTestIT extends AbstractTestObject {

    private static final Logger LOGGER = Logger.getLogger(
            T1_SamplesTestIT.class.getPackage().getName());

    private static final String CLASS_NAME = T1_SamplesTestIT.class
            .getSimpleName();

    @Test
    public void uas_sample_test()
            throws GlassFishException, InterruptedException, BundleException,
            IOException {

        LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test", "ENTRY");
        TestContext tc = TestContext.create(getClass());
        try {
            Bundle uas_api_b = tc.installBundle(sampleAppLocation("uas-api"));
            Bundle uas_simpleservice_b = tc.installBundle(
                    sampleAppLocation("uas-simpleservice"));
            Bundle uas_simplewab_b = tc.installBundle(
                    sampleAppLocation("uas-simplewab", "war"));
            WebAppBundle uas_simple_webapp = new WebAppBundle(ctx,
                    uas_simplewab_b);
            uas_simple_webapp.deploy(getTimeout(), TimeUnit.MILLISECONDS);
            String response = null;

            // Service type of the EJB registered by uas ejb service bundle
            final String uas_service_type = "org.glassfish.fighterfish.sample.uas.api.UserAuthService";

            // Various request URIs - very tightly dependent on servlets
            // implementing the functionality
            final String loginRequest = "/LoginServlet?name=foo&password=bar";
            final String registrationRequest = "/RegistrationServlet?name=foo&password=bar";
            final String unregistrationRequest = "/UnregistrationServlet?name=foo";
            final String reportJspRequest = "/report.jsp";
            final String reportServletRequest = "/ReportServlet";

            // Expected Patterns for various kinds of output - very tightly
            // coupled with what the implementation returns
            final String serviceUnavailable = "Service is not yet available";
            final String loginFailed = "Incorrect user name or password. Try again";
            final String successfulLogin = "Welcome ";
            final String successfulRegistration = "Registered ";
            final String successfulUnregistration = "Unregistered ";

            // Scenario 1: no service
            response = uas_simple_webapp.getHttpGetResponse(loginRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    serviceUnavailable));

            // Scenario 2: dynamically adding a service bundle and retrying.
            uas_simpleservice_b.start(START_TRANSIENT);
            response = uas_simple_webapp.getHttpGetResponse(loginRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(loginFailed));

            // now let's register a user and retry
            response = uas_simple_webapp.getHttpPostResponse(
                    registrationRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    successfulRegistration));
            response = uas_simple_webapp.getHttpGetResponse(loginRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    successfulLogin));

            // unregister
            response = uas_simple_webapp.getHttpPostResponse(
                    unregistrationRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    successfulUnregistration));

            // Scenario #3: Dynamically switching the service by ejbservice
            uas_simpleservice_b.stop();
            response = uas_simple_webapp.getHttpGetResponse(loginRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    serviceUnavailable));

            // let's install ejbservice bundle and retry
            Bundle uas_ejbservice_b = tc.installBundle(
                    sampleAppLocation("uas-ejbservice"));
            EjbBundle uas_ejbapp = new EjbBundle(ctx, uas_ejbservice_b,
                    new String[]{uas_service_type});
            uas_ejbapp.deploy(getTimeout(), TimeUnit.MILLISECONDS);
            // unregister just in case there was a user by this name
            uas_simple_webapp.getHttpPostResponse(unregistrationRequest);
            response = uas_simple_webapp.getHttpGetResponse(loginRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(loginFailed));

            // now let's register a user and retry
            response = uas_simple_webapp.getHttpPostResponse(
                    registrationRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    successfulRegistration));

            response = uas_simple_webapp.getHttpGetResponse(loginRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(successfulLogin));

            // unregister
            response = uas_simple_webapp.getHttpPostResponse(
                    unregistrationRequest);

            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    successfulUnregistration));

            // stop the service bundle and retry to make sure we are
            // failing to get the service
            uas_ejbapp.undeploy();
            response = uas_simple_webapp.getHttpPostResponse(
                    unregistrationRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(serviceUnavailable));

            // Scenario #4: Let's replace the ejbservice by ejbservice2
            // which uses standalone entities jar.
            Bundle uas_entity_b = tc.installBundle(
                    sampleAppLocation("uas-entities"));
            EntityBundle uas_entityapp = tc.deployEntityBundle(uas_entity_b);
            Bundle uas_ejbservice2_b = tc.installBundle(
                    sampleAppLocation("uas-ejbservice2"));
            EjbBundle uas_ejbapp2 = tc.deployEjbBundle(uas_ejbservice2_b,
                    new String[]{uas_service_type});
            response = uas_simple_webapp.getHttpPostResponse(
                    registrationRequest);
            assertThat(response, new StringPatternMatcher(
                    successfulRegistration));

            // login
            response = uas_simple_webapp.getHttpGetResponse(loginRequest);
            assertThat(response, new StringPatternMatcher(successfulLogin));

            // unregister
            response = uas_simple_webapp.getHttpPostResponse(
                    unregistrationRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    successfulUnregistration));

            // WAB fragment test
            try {
                uas_simple_webapp.getHttpGetResponse(reportJspRequest);
                fail("Expected fragment to be not available");
            } catch (IOException e) {
                Assert.assertTrue("Expected FileNotFoundException",
                        e instanceof FileNotFoundException);
            }

            // now install the fragment and refresh the host
            tc.installBundle(sampleAppLocation("uas-simplewabfragment"));
            // This is needed so that the web app
            // does not get deployed upon update().
            uas_simplewab_b.stop();
            uas_simplewab_b.update();
            // TODO(Sahoo): because of some bug, we can't reuse earlier wab
            uas_simple_webapp = new WebAppBundle(ctx, uas_simplewab_b);
            // deploy again
            uas_simple_webapp.deploy(getTimeout(), TimeUnit.MILLISECONDS);
            response = uas_simple_webapp.getHttpGetResponse(
                    reportJspRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    "to see the report."));

            // now let's see if the servlet from the fragment can be used
            // or not.
            response = uas_simple_webapp.getHttpGetResponse(
                    reportServletRequest);
            LOGGER.logp(Level.INFO, CLASS_NAME, "uas_sample_test",
                    "response = {0}", new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    "Login Attempt Report:"));
        } finally {
            tc.destroy();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void osgihttp_helloworld_sample_test() throws GlassFishException,
            InterruptedException, BundleException, IOException {

        LOGGER.logp(Level.INFO, CLASS_NAME,
                "osgihttp_helloworld_sample_test", "ENTRY");
        TestContext tc = TestContext.create(getClass());
        try {
            HttpService httpService = OSGiUtil.getService(ctx,
                    HttpService.class);
            assertNull(httpService);
            for (Bundle b : ctx.getBundles()) {
                if ("org.glassfish.fighterfish.osgi-http".equals(
                        b.getSymbolicName())) {
                    b.stop(Bundle.START_TRANSIENT);
                    b.start(Bundle.START_TRANSIENT);
                }
            }
            httpService = OSGiUtil.getService(ctx, HttpService.class,
                    getTimeout());
            assertNotNull(httpService);
            Bundle bundle = tc.installBundle(
                    sampleAppLocation("osgihttp-helloworld"));
            final Semaphore eventRaised = new Semaphore(0);
            EventAdmin eventAdmin = OSGiUtil.getService(ctx, EventAdmin.class,
                    getTimeout());
            Assert.assertNotNull("Event Admin Service not available",
                    eventAdmin);
            Properties props = new Properties();
            String[] topics = {
                "org/glassfish/fighterfish/sample/osgihttp/helloworld"
            };
            props.put(EventConstants.EVENT_TOPIC, topics);
            ctx.registerService(EventHandler.class.getName(),
                    new EventHandler() {
                @Override
                public void handleEvent(Event event) {
                    LOGGER.logp(Level.INFO, "SingleTest", "handleEvent",
                            "event = {0}", new Object[]{event});
                    eventRaised.release();
                }
            }, (Dictionary) props);

            bundle.start(Bundle.START_TRANSIENT);
            assertTrue("Timedout waiting for event", eventRaised.tryAcquire(1,
                    getTimeout(), TimeUnit.MILLISECONDS));
            URL request1 = new URL("http://localhost:8080/osgi/hello1");
            URL request2 = new URL("http://localhost:8080/osgi/hello2");
            URL request3 = new URL("http://localhost:8080/osgi/hello3");
            String response = getResponse(request1);
            LOGGER.logp(Level.INFO, CLASS_NAME,
                    "osgihttp_helloworld_sample_test", "response = {0}",
                    new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    "servlet context counter = 0"));
            response = getResponse(request1);
            LOGGER.logp(Level.INFO, CLASS_NAME,
                    "osgihttp_helloworld_sample_test", "response = {0}",
                    new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    "servlet context counter = 1"));
            response = getResponse(request2);
            LOGGER.logp(Level.INFO, CLASS_NAME,
                    "osgihttp_helloworld_sample_test", "response = {0}",
                    new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    "servlet context counter = 2"));
            response = getResponse(request1);
            LOGGER.logp(Level.INFO, CLASS_NAME,
                    "osgihttp_helloworld_sample_test", "response = {0}",
                    new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    "servlet context counter = 3"));
            response = getResponse(request3);
            LOGGER.logp(Level.INFO, CLASS_NAME,
                    "osgihttp_helloworld_sample_test", "response = {0}",
                    new Object[]{response});
            assertThat(response, new StringPatternMatcher(
                    "servlet context counter = null"));
        } finally {
            tc.destroy();
        }
    }

    @Test
    public void jaxrs_sample_test() throws GlassFishException,
            InterruptedException, BundleException, IOException {
        LOGGER.logp(Level.INFO, CLASS_NAME, "jaxrs_sample_test", "ENTRY");
        TestContext tc = TestContext.create(getClass());
        try {
            final String registrationRequest = "/register?name=admin&password=admin";
            final String successfulRegistration = "Registered";
            final String loginRequest = "/login?name=admin&password=admin";
            final String successfulLogin = "Logged in";

            Bundle uas_api_b = tc.installBundle(sampleAppLocation("uas-api"));
            uas_api_b.start();
            Bundle uas_simpleservice_b = tc.installBundle(
                    sampleAppLocation("uas-simpleservice"));
            uas_simpleservice_b.start();
            Bundle uas_simplejaxwab_b = tc.installBundle(
                    sampleAppLocation("uas-simplejaxrs", "war"));
            WebAppBundle uas_simple_jaxwebapp = new WebAppBundle(ctx,
                    uas_simplejaxwab_b);
            uas_simple_jaxwebapp.deploy(getTimeout(), TimeUnit.MILLISECONDS);
            String response = null;
            {
                //  register a user
                response = uas_simple_jaxwebapp.getHttpPostResponse(
                        registrationRequest, "text/plain");
                LOGGER.logp(Level.INFO, CLASS_NAME, "uas_jaxsample_test",
                        "response = {0}", new Object[]{response});
                assertThat(response, new StringPatternMatcher(
                        successfulRegistration));
                //  login the user
                response = uas_simple_jaxwebapp.getHttpGetResponse(loginRequest);
                LOGGER.logp(Level.INFO, CLASS_NAME, "uas_jaxsample_test",
                        "response = {0}", new Object[]{response});
                assertThat(response, new StringPatternMatcher(successfulLogin));
            }
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
