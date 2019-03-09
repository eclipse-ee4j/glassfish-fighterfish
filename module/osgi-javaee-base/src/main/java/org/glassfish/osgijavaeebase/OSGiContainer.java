/*
 * Copyright (c) 2009, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgijavaeebase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import static org.osgi.framework.Constants.ACTIVATION_LAZY;
import static org.osgi.framework.Constants.BUNDLE_ACTIVATIONPOLICY;

/**
 * This class is primarily responsible for deployment and undeployment of EE
 * artifacts of an OSGi bundle.
 */
public class OSGiContainer {

    /**
     * Context in which this object is operating.
     */
    private BundleContext context;

    /**
     * Applications managed.
     */
    private final Map<Bundle, OSGiApplicationInfo> applications
            = new HashMap<Bundle, OSGiApplicationInfo>();

    /**
     * Services registered by applications.
     */
    private final Map<OSGiApplicationInfo, ServiceRegistration> regs
            = new HashMap<OSGiApplicationInfo, ServiceRegistration>();

    /**
     * Service tracker for deployer service.
     */
    private ServiceTracker deployerTracker;

    /**
     * Sorted in descending order of service ranking.
     */
    private final List<ServiceReference/*OSGiDeployer*/> sortedDeployerRefs
            = new ArrayList<ServiceReference>();

    /**
     * Flag to track shutdown state.
     */
    private boolean shutdown = false;

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(
            OSGiContainer.class.getPackage().getName());

    /**
     * Create a new instance.
     * @param ctx bundle context
     */
    protected OSGiContainer(final BundleContext ctx) {
        this.context = ctx;
        deployerTracker = new OSGiDeployerTracker();
    }

    /**
     * Init the container.
     */
    protected void init() {
        // no need to deployAll, as that will happen when tracker is notified
        // of each deployer.
        deployerTracker.open();
    }

    /**
     * Get the applications currently deployed.
     * @return map of bundle to application info
     */
    protected Map<Bundle, OSGiApplicationInfo> getApplications() {
        return applications;
    }

    /**
     * Get the service registered by the applications currently deployed.
     * @return map of application info to service registration
     */
    protected Map<OSGiApplicationInfo, ServiceRegistration> getRegs() {
        return regs;
    }

    /**
     * Shutdown the container.
     */
    protected synchronized void shutdown() {
        undeployAll();
        assert (applications.isEmpty() && regs.isEmpty());
        applications.clear();
        regs.clear();
        sortedDeployerRefs.clear();
        shutdown = true;
        deployerTracker.close();
        deployerTracker = null;
        context = null;
    }

    /**
     * Test if the container is shutdown.
     * @return {@code true} if shutdown, {@code false} otherwise
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Redeploy the given application.
     * @param bundle the bundle of the application
     * @return the new application info
     * @throws Exception if an error occurs
     */
    private synchronized OSGiApplicationInfo redeploy(final Bundle bundle)
            throws Exception {

        if (isShutdown()) {
            return null;
        }
        if (isDeployed(bundle)) {
            undeploy(bundle);
        }
        return deploy(bundle);
    }

    /**
     * Deploys an application bundle in underlying application container in
     * GlassFish.This method is synchronized because we don't know if GlassFish
     * deployment framework can handle concurrent requests or not.
     *
     * @param b Bundle to be deployed.
     * @return {@link OSGiApplicationInfo} instance
     */
    @SuppressWarnings("unchecked")
    public synchronized OSGiApplicationInfo deploy(final Bundle b) {
        if (isShutdown()) {
            return null;
        }
        // By the time this extender is processing the bundle, if the bundle
        // has already changed
        // state to STOPPING, then cancel the deployment operation.
        if (b.getState() == Bundle.STOPPING) {
            LOGGER.logp(Level.INFO, "OSGiContainer", "deploy",
                    "Bundle {0} is already moved to STOPPING state"
                            + "so it won't be deployed.",
                    new Object[]{b});
            return null;
        }
        OSGiApplicationInfo osgiAppInfo = applications.get(b);
        if (osgiAppInfo != null) {
            LOGGER.logp(Level.WARNING, "OSGiContainer", "deploy",
                    "Bundle {0} is already deployed at {1} ", new Object[]{b,
                        osgiAppInfo.getAppInfo().getSource()});
            return null;
        }
        ServiceReference/*OSGiDeployer*/ osgiDeployerRef = selectDeployer(b);
        if (osgiDeployerRef == null) {
            // No deployer recognises this bundle, so return
            return null;
        }
        OSGiDeployer osgiDeployer = (OSGiDeployer) context
                .getService(osgiDeployerRef);
        if (osgiDeployer == null) {
            LOGGER.logp(Level.WARNING, "OSGiContainer", "deploy",
                    "Bundle {0} can't be deployed because corresponding"
                            + " deployer {1} has vanished!",
                    new Object[]{b,
                        osgiDeployer});
            return null;
        }

        // deploy the java ee artifacts
        try {
            osgiAppInfo = osgiDeployer.deploy(b);
        } catch (Exception e) {
            LOGGER.logp(Level.WARNING, "OSGiContainer", "deploy",
                    "Failed to deploy bundle " + b, e);
            return null;
        }
        osgiAppInfo.setDeployer(osgiDeployerRef);
        applications.put(b, osgiAppInfo);
        ServiceRegistration reg = context.registerService(
                OSGiApplicationInfo.class.getName(), osgiAppInfo,
                (Dictionary) new Properties());
        regs.put(osgiAppInfo, reg);
        LOGGER.logp(Level.INFO, "OSGiContainer", "deploy",
                "deployed bundle {0} at {1}",
                new Object[]{
                    osgiAppInfo.getBundle(),
                    osgiAppInfo.getAppInfo().getSource().getURI()
                });
        return osgiAppInfo;
    }

    /**
     * Undeploys a Java EE application bundle. This method is synchronized
     * because we don't know if GlassFish deployment framework can handle
     * concurrent requests or not.
     *
     * @param bundle Bundle to be undeployed
     */
    @SuppressWarnings("unchecked")
    public synchronized void undeploy(final Bundle bundle) {
        if (isShutdown()) {
            return;
        }
        OSGiApplicationInfo osgiAppInfo = applications.get(bundle);
        if (osgiAppInfo == null) {
            throw new RuntimeException("No applications for bundle " + bundle);
        }
        applications.remove(bundle);
        regs.remove(osgiAppInfo).unregister();
        ServiceReference osgiDeployerRef = osgiAppInfo.getDeployer();
        OSGiDeployer osgiDeployer = (OSGiDeployer) context
                .getService(osgiDeployerRef);
        if (osgiDeployer == null) {
            LOGGER.logp(Level.WARNING, "OSGiContainer", "undeploy",
                    "Failed to undeploy {0}, because corresponding deployer"
                    + " does not exist",
                    new Object[]{bundle});
            return;
        }
        try {
            osgiDeployer.undeploy(osgiAppInfo);
            LOGGER.logp(Level.INFO, "OSGiContainer", "undeploy",
                    "Undeployed bundle {0}", new Object[]{bundle});
        } catch (Exception e) {
            LOGGER.logp(Level.WARNING, "OSGiContainer", "undeploy",
                    "Failed to undeploy bundle " + bundle, e);
        }
    }

    /**
     * Undeploys all applications.
     */
    public synchronized void undeployAll() {
        // Take a copy of the entries as undeploy changes the underlying map.
        for (Bundle b : new HashSet<Bundle>(applications.keySet())) {
            try {
                undeploy(b);
            } catch (Exception e) {
                LOGGER.logp(Level.SEVERE, "OSGiContainer", "undeployAll",
                        "Exception undeploying bundle " + b,
                        e);
            }
        }
    }

    /**
     * Test if the application bundle is deployed.
     * @param bundle the bundle of the application to test
     * @return {@code true} if deployed, {@code false} otherwise
     */
    public synchronized boolean isDeployed(final Bundle bundle) {
        return applications.containsKey(bundle);
    }

    /**
     * Test if the application bundle is in active state, or starting if the
     *  bundle is configured to be lazy.
     * @param bundle the bundle of the application to test
     * @return {@code true} if ready, {@code false} otherwise
     */
    final boolean isReady(final Bundle bundle) {
        final int state = bundle.getState();
        final boolean isActive = (state & Bundle.ACTIVE) != 0;
        final boolean isStarting = (state & Bundle.STARTING) != 0;
        final boolean isReady = isActive || (isLazy(bundle) && isStarting);
        return isReady;
    }

    /**
     * Test if the application bundle is lazy.
     * @param bundle the bundle of the application to test
     * @return {@code true} if lazy, {@code false} otherwise
     */
    static boolean isLazy(final Bundle bundle) {
        return ACTIVATION_LAZY.equals(
                bundle.getHeaders().get(BUNDLE_ACTIVATIONPOLICY));
    }

    /**
     * Select the deployer for the given bundle.
     * @param bundle the application bundle
     * @return the deployer service reference if found, or {@code null}
     */
    @SuppressWarnings("unchecked")
    private ServiceReference selectDeployer(final Bundle bundle) {
        // deployerRefs is already sorted in descending order of ranking
        for (ServiceReference deployerRef : sortedDeployerRefs) {
            OSGiDeployer deployer = OSGiDeployer.class.cast(context
                    .getService(deployerRef));
            if (deployer != null) {
                if (deployer.handles(bundle)) {
                    return deployerRef;
                }
            }
        }
        return null;
    }

    /**
     * Get the applications currently deployed.
     * @return array of application info
     */
    public synchronized OSGiApplicationInfo[] getDeployedApps() {
        // must return a snapshot, because it is used from
        // DeployerRemovedThread.
        return applications.values().toArray(new OSGiApplicationInfo[0]);
    }

    /**
     * Custom service tracker to track {@link OSGiDeployer} service instances.
     */
    private class OSGiDeployerTracker extends ServiceTracker {

        /**
         * Create a new instance.
         */
        @SuppressWarnings("unchecked")
        OSGiDeployerTracker() {
            super(OSGiContainer.this.context, OSGiDeployer.class.getName(),
                    null);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object addingService(final ServiceReference reference) {
            deployerAdded(reference);
            return super.addingService(reference);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void removedService(final ServiceReference reference,
                final Object service) {

            deployerRemoved(reference);
            super.removedService(reference, service);
        }

    }

    /**
     * Add the service reference of a deployer service instance that was added.
     * @param reference the deployer service reference
     */
    private synchronized void deployerAdded(final ServiceReference reference) {
        if (isShutdown()) {
            return;
        }
        sortedDeployerRefs.add(reference);
        // descending order
        Collections.sort(sortedDeployerRefs, Collections.reverseOrder());
        new DeployerAddedThread(reference).start();
    }

    /**
     * Remove the service reference of a deployer service instance that was
     * removed.
     * @param reference the deployer service reference
     */
    private void deployerRemoved(final ServiceReference reference) {
        if (isShutdown()) {
            return;
        }
        sortedDeployerRefs.remove(reference);
        new DeployerRemovedThread(reference).start();
    }

    /**
     * A thread to re-deploy the bundles handled by a given deployer when it is
     *  added.
     */
    private final class DeployerAddedThread extends Thread {

        /**
         * The deployer service reference.
         */
        private final ServiceReference newDeployerRef;

        /**
         * Create a new instance.
         * @param ref the new deployer service reference
         */
        private DeployerAddedThread(final ServiceReference ref) {
            this.newDeployerRef = ref;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            synchronized (OSGiContainer.this) {
                OSGiDeployer newDeployer = (OSGiDeployer) context
                        .getService(newDeployerRef);
                if (newDeployer == null) {
                    return;
                }
                for (Bundle b : context.getBundles()) {
                    if (isReady(b) && newDeployer.handles(b)) {
                        try {
                            redeploy(b);
                        } catch (Exception e) {
                            LOGGER.logp(Level.WARNING, "OSGiContainer",
                                    "addingService",
                                    "Exception redeploying bundle " + b, e);
                        }
                    }
                }
            }
        }
    }

    /**
     * A thread to re-deploy the applications handled by a given deployer
     * when it is removed.
     */
    private final class DeployerRemovedThread extends Thread {

        /**
         * The deployer service reference.
         */
        private final ServiceReference oldDeployerRef;

        /**
         * Create a new instance.
         * @param ref the old deployer service reference
         */
        private DeployerRemovedThread(final ServiceReference ref) {
            this.oldDeployerRef = ref;
        }

        @Override
        public void run() {
            synchronized (OSGiContainer.this) {
                // getDeployedApps returns a snapshot which is essential
                // because redeploy() changes the collection.
                for (OSGiApplicationInfo osgiApplicationInfo
                        : getDeployedApps()) {
                    if (osgiApplicationInfo.getDeployer() == oldDeployerRef) {
                        try {
                            redeploy(osgiApplicationInfo.getBundle());
                        } catch (Exception e) {
                            LOGGER.logp(Level.WARNING, "DeployerRemovedThread",
                                    "run", "Exception redeploying bundle "
                                            + osgiApplicationInfo.getBundle(),
                                    e);
                        }
                    }
                }
            }
        }
    }
}
