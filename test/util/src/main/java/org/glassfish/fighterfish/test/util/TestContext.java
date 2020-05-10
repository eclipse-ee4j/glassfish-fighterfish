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
package org.glassfish.fighterfish.test.util;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.INFO;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * A TestContext is a facade through which a test interacts with underlying OSGi or Java EE platform. It provides
 * functionality like installing/uninstalling bundles, configuring Java EE resources like JDBC data sources, JMS
 * destinations, etc. Each test method is accompanied by a single TestContext object. 
 * 
 * <p>
 * A TestContext object's life cycle
 * is scoped to a test method for this reason. Each test method must create a TestContext by calling the factory method
 * {@link #create(Class)} at the beginning of the test method and destroy it by calling {@link #destroy} at the end of
 * the test method. When a test context is destroyed, all changes done so far will be rolled back. This includes any
 * bundles deployed. any domain configuration made, etc.
 */
public final class TestContext implements AutoCloseable {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(TestContext.class.getPackage().getName());

    // TODO(Sahoo): Group related methods into separate interfaces.
    // TODO(Sahoo): Add methods from OSGiUtil here.
    // TODO(Sahoo): Use fluent API
    // TODO(Sahoo): Explore possibility of automatically controlling life cycle
    // of a TestContext
    /**
     * Test ID.
     */
    private final String testID;

    /**
     * Test ID generator.
     */
    private static final AtomicInteger TEST_ID_GEN = new AtomicInteger(0);

    /**
     * BundleContext associated with the test.
     */
    private final BundleContext ctx;

    /**
     * Bundle provisioner.
     */
    private final BundleProvisioner bundleProvisioner;

    /**
     * Resource bundle provisioner.
     */
    private final EnterpriseResourceProvisioner resourceProvisioner;

    /**
     * Create a new instance.
     * 
     * @param id test id
     * @param bndCtx bundle contexct
     */
    private TestContext(String id, BundleContext bndCtx) {
        LOGGER.log(INFO, "Creating test context for test id: {0}", id);
        
        this.ctx = bndCtx;
        this.testID = id;
        bundleProvisioner = new BundleProvisioner(bndCtx);
        resourceProvisioner = new EnterpriseResourceProvisioner(bndCtx);
    }

    /**
     * Create a test context.
     * 
     * @param testClass test class
     * @return TestContext
     * @throws GlassFishException if an error occurs
     * @throws InterruptedException if an error occurs
     */
    public static TestContext create(Class testClass) throws GlassFishException, InterruptedException {
        BundleContext bundleContext = FrameworkUtil.getBundle(testClass).getBundleContext();
        
        TestContext testContext = new TestContext(getNextTestId(testClass), bundleContext);
        testContext.getGlassFish();
        testContext.configureEmbeddedDerby();
        
        return testContext;
    }

    /**
     * Get the next test id.
     * 
     * @param testClass test class
     * @return String
     */
    private static String getNextTestId(Class<?> testClass) {
        // Don't use something : as that interefers with asadmin command syntax
        return testClass.getName() + "-" + String.valueOf(TEST_ID_GEN.incrementAndGet());
    }
    
    @Override
    public void close() throws BundleException, GlassFishException {
        destroy();
    }

    /**
     * Destroy the test context.
     * 
     * @throws BundleException if an error occurs
     * @throws GlassFishException if an error occurs
     */
    public void destroy() throws BundleException, GlassFishException {
        LOGGER.log(INFO, "Destroying test context for test id: {0}", testID);
        
        try {
            bundleProvisioner.uninstallAllTestBundles();
        } finally {
            resourceProvisioner.restoreDomainConfiguration();
        }
    }

    /**
     * Deploy the given OSGi Web Application Bundle. WAB deployment happens asynchronously when a WAB is activated. It waits
     * for a configured amount time for deployment to take place successfully. If deployment fails or does not happen within
     * the configured times, it throws TimeoutException.
     *
     * @param bundle bundle to deploy
     * @return ServletContext associated with the deployed web application
     * @throws BundleException if an error occurs
     * @throws InterruptedException if an error occurs while waiting
     */
    public WebAppBundle deployWebAppBundle(Bundle bundle) throws BundleException, InterruptedException {
        WebAppBundle webAppBundle = new WebAppBundle(getBundleContext(), bundle);
        webAppBundle.deploy(TestsConfiguration.getInstance().getTimeout(), MILLISECONDS);
        
        return webAppBundle;
    }

    /**
     * Deploy the given OSGi Web Application bundle with the given bundle location.
     * 
     * @param location bundle location
     * @return WebAppBundle
     * @throws BundleException if an error occurs
     * @throws InterruptedException if an error occurs while waiting
     */
    public WebAppBundle deployWebAppBundle(final String location) throws BundleException, InterruptedException {
        return deployWebAppBundle(installBundle(location));
    }

    /**
     * Deploy the given JPA Entities bundle. If a service of type EntityManagerFactory does not get registered in the
     * specified time, assume the deployment has failed and throw a TimeoutException.
     *
     * @param bundle Entity bundle to be deployed
     * @return a handle to the deployed application
     * @throws BundleException if an error occurs
     * @throws InterruptedException if an error occurs while waiting
     */
    public EntityBundle deployEntityBundle(final Bundle bundle) throws BundleException, InterruptedException {
        EntityBundle entityBundle = new EntityBundle(getBundleContext(), bundle);
        entityBundle.deploy(TestsConfiguration.getInstance().getTimeout(), MILLISECONDS);
        
        return entityBundle;
    }

    /**
     * Deploy the given entity bundle.
     * 
     * @param location bundle location
     * @return EntityBundle
     * @throws BundleException if an error occurs
     * @throws InterruptedException if an error occurs while waiting
     */
    public EntityBundle deployEntityBundle(final String location) throws BundleException, InterruptedException {
        return deployEntityBundle(installBundle(location));
    }

    /**
     * Deploy the given EJB OSGi bundle. Deployment is triggered asynchronously by starting the bundle. If none of the user
     * specified services show up in service registry in the specified amount of time, it assumes the operation has failed
     * and throws TimeoutOperation.
     *
     * @param bundle EJB Bundle to be deployed
     * @param services Services that are expected to be made available by this EJB bundle if deployment is successful.
     * @return a handle to the deployed application
     * @throws BundleException if an error occurs
     * @throws InterruptedException if an error occurs while waiting
     */
    public EjbBundle deployEjbBundle(final Bundle bundle, final String[] services) throws BundleException, InterruptedException {

        EjbBundle ejbBundle = new EjbBundle(getBundleContext(), bundle, services);
        ejbBundle.deploy(TestsConfiguration.getInstance().getTimeout(), MILLISECONDS);
        return ejbBundle;
    }

    /**
     * Deploy the given EJB OSGi bundle by bundle location.
     * 
     * @param location bundle location
     * @param services services provided by the bundle
     * @return EjbBundle
     * @throws BundleException if an error occurs
     * @throws InterruptedException if an error occurs while waiting
     */
    public EjbBundle deployEjbBundle(final String location, final String[] services) throws BundleException, InterruptedException {

        return deployEjbBundle(installBundle(location), services);
    }

    /**
     * Get the GlassFish service.
     * 
     * @return GlassFish
     * @throws GlassFishException if an error occurs
     * @throws InterruptedException if an error occurs while waiting
     */
    public GlassFish getGlassFish() throws GlassFishException, InterruptedException {

        return GlassFishTracker.waitForGfToStart(ctx, TestsConfiguration.getInstance().getTimeout());
    }

    /**
     * Configure embedded derby.
     * 
     * @throws GlassFishException if an error occurs
     * @throws InterruptedException if an error occurs while waiting
     */
    public void configureEmbeddedDerby() throws GlassFishException, InterruptedException {
        resourceProvisioner.configureEmbeddedDerby(getGlassFish(), testID, testID);
    }

    /**
     * Get the bundle context.
     * 
     * @return BundleContext
     */
    public BundleContext getBundleContext() {
        return ctx;
    }

    /**
     * Install an OSGi bundle by reading its content from a given location URI. This method does not activate the bundle; it
     * just installs it.
     *
     * @param location a URI string from which the bundle content will be read
     * @return installed bundle object
     * @throws BundleException if an error occurs
     */
    public Bundle installBundle(final String location) throws BundleException {
        return bundleProvisioner.installTestBundle(location);
    }

    /**
     * Create a JMS connection factory.
     * 
     * @param cfName factory name
     * @throws GlassFishException if an error occurs
     * @throws InterruptedException if an error occurs while waiting
     */
    public void createJmsCF(final String cfName) throws GlassFishException, InterruptedException {
        resourceProvisioner.createJmsCF(getGlassFish(), cfName);
    }

    /**
     * Create a JMS topic.
     * 
     * @param topicName topic name
     * @throws GlassFishException if an error occurs
     * @throws InterruptedException if an error occurs while waiting
     */
    public void createJmsTopic(final String topicName) throws GlassFishException, InterruptedException {
        resourceProvisioner.createJmsTopic(getGlassFish(), topicName);
    }
    
}
