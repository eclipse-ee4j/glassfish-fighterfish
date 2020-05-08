/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * This extender is responsible for detecting and deploying any Java EE OSGi bundle. Implementation Note: All methods
 * are synchronized, because we don't allow the extender to stop while it is deploying or undeploying something.
 * Similarly, while it is being stopped, we don't want it to deploy or undeploy something. After receiving the event, it
 * spawns a separate thread to carry out the task so that we don't spend long time in the synchronous event listener.
 * More over, that can lead to deadlocks as observed in https://glassfish.dev.java.net/issues/show_bug.cgi?id=14313.
 */
public final class JavaEEExtender implements Extender {

    /**
     * Property name for deployment timeout configuration.
     */
    private static final String DEPLOYMENT_TIMEOUT = "org.glassfish.osgijavaeebase.deployment.timeout";

    /**
     * Default deployment timeout value.
     */
    private static final long DEFAULT_DEPLOYMENT_TIMEOUT = 10000;

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(JavaEEExtender.class.getPackage().getName());

    /**
     * Bundle context.
     */
    private final BundleContext context;

    /**
     * OSGi container.
     */
    private volatile OSGiContainer c;

    /**
     * Service registration of the OSGi container.
     */
    private ServiceRegistration reg;

    /**
     * Bundle tracker.
     */
    private BundleTracker tracker;

    /**
     * Executor service.
     */
    private ExecutorService executorService;

    /**
     * Create a new instance.
     *
     * @param bundleContext bundle context
     */
    public JavaEEExtender(final BundleContext bundleContext) {
        this.context = bundleContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void start() {
        executorService = Executors.newSingleThreadExecutor();
        c = new OSGiContainer(context);
        c.init();
        reg = context.registerService(OSGiContainer.class.getName(), c, null);
        tracker = new BundleTracker(context, Bundle.ACTIVE | Bundle.STARTING, new HybridBundleTrackerCustomizer());
        tracker.open();
    }

    @Override
    public synchronized void stop() {
        if (c == null) {
            return;
        }
        OSGiContainer tmp = c;
        c = null;
        tmp.shutdown();
        if (tracker != null) {
            tracker.close();
        }
        tracker = null;
        reg.unregister();
        reg = null;
        executorService.shutdownNow();
    }

    /**
     * Initiate the deployment action of the given bundle against the OSGi container.
     *
     * @param bundle bundle to be deployed
     * @return OSGiApplicationInfo or {@code null} if the container is not started or if an error occurs during deployment
     */
    private synchronized OSGiApplicationInfo deploy(final Bundle bundle) {
        if (!isStarted()) {
            return null;
        }
        try {
            return c.deploy(bundle);
        } catch (Throwable e) {
            LOGGER.logp(Level.SEVERE, "JavaEEExtender", "deploy", "Exception deploying bundle {0}", new Object[] { bundle.getLocation() });
            LOGGER.logp(Level.SEVERE, "JavaEEExtender", "deploy", "Exception Stack Trace", e);
        }
        return null;
    }

    /**
     * Initiate the undeployment action of the given bundle against the OSGi container.
     *
     * @param bundle bundle to be deployed
     */
    private synchronized void undeploy(final Bundle bundle) {
        if (!isStarted()) {
            return;
        }
        try {
            if (c.isDeployed(bundle)) {
                c.undeploy(bundle);
            }
        } catch (Exception e) {
            LOGGER.logp(Level.SEVERE, "JavaEEExtender", "undeploy", "Exception undeploying bundle {0}", new Object[] { bundle.getLocation() });
            LOGGER.logp(Level.SEVERE, "JavaEEExtender", "undeploy", "Exception Stack Trace", e);
        }
    }

    /**
     * Test if the OSGi container is started.
     *
     * @return {@code true} if started, {@code false} otherwise
     */
    private boolean isStarted() {
        // This method is deliberately made non-synchronized, because it is
        // called from tracker customizer
        return c != null;
    }

    /**
     * A bundle tracker customizer to deploy asynchronously.
     */
    private class HybridBundleTrackerCustomizer implements BundleTrackerCustomizer {

        /**
         * The deployment tasks executed asynchronously.
         */
        private final Map<Long, Future<OSGiApplicationInfo>> deploymentTasks = new ConcurrentHashMap<>();

        @Override
        public Object addingBundle(final Bundle bundle, final BundleEvent event) {

            if (!isStarted()) {
                return null;
            }
            final int state = bundle.getState();
            if (isReady(event, state)) {
                Future<OSGiApplicationInfo> future = executorService.submit(new Callable<OSGiApplicationInfo>() {
                    @Override
                    public OSGiApplicationInfo call() throws Exception {
                        return deploy(bundle);
                    }
                });
                deploymentTasks.put(bundle.getBundleId(), future);
                return bundle;
            }
            return null;
        }

        /**
         * Bundle is ready when its state is ACTIVE or, when a lazy activation policy is used, STARTING.
         *
         * @param event the bundle event
         * @param state the bundle state
         * @return {@code true} if ready, {@code false} otherwise
         */
        private boolean isReady(final BundleEvent event, final int state) {
            return state == Bundle.ACTIVE || state == Bundle.STARTING && event != null && event.getType() == BundleEvent.LAZY_ACTIVATION;
        }

        @Override
        public void modifiedBundle(final Bundle bundle, final BundleEvent event, final Object object) {
        }

        @Override
        public void removedBundle(final Bundle bundle, final BundleEvent event, final Object object) {

            if (!isStarted()) {
                return;
            }
            Future<OSGiApplicationInfo> deploymentTask = deploymentTasks.remove(bundle.getBundleId());
            if (deploymentTask == null) {
                // We have never seen this bundle before. Ideally we should
                // never get here.
                assert false;
                return;
            }
            try {
                OSGiApplicationInfo deployedApp = null;
                try {
                    deployedApp = deploymentTask.get(getDeploymentTimeout(), TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    LOGGER.logp(Level.FINE, "JavaEEExtender$HybridBundleTrackerCustomizer", "removedBundle",
                            "Undeployer times out waiting for deployment to" + " finish for bundle " + bundle, e);
                    boolean isCancelled = deploymentTask.cancel(true);
                    if (!isCancelled) {
                        // cancellation of timer won't be successful if the
                        // deployer has finished by the time we attempt to
                        // cancel
                        deployedApp = deploymentTask.get();
                    } else {
                        LOGGER.logp(Level.INFO, "JavaEEExtender$HybridBundleTrackerCustomizer", "removedBundle", "isCancelled = {0}",
                                new Object[] { isCancelled });
                    }
                }
                // It is not sufficient to check the future only, as the
                // DeployerAddedThread currently deploys
                // without our knowledge, so we must also check isDeployed().
                // More over, if the task has been cancelled, deployedApp will
                // be null, but the deployer might have
                // almost deployed the bundle as seen issue GLASSFISH-18159.
                // In such case, we have to check the
                // deployment status by calling isDeployed().
                if (deployedApp != null || c.isDeployed(bundle)) {
                    // undeploy synchronously to avoid any deadlock.
                    undeploy(bundle);
                }
            } catch (InterruptedException e) {
                // TODO(Sahoo): Proper Exception Handling
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                LOGGER.logp(Level.FINE, "JavaEEExtender$HybridBundleTrackerCustomizer", "removedBundle", "e = {0}", new Object[] { e });
            }
        }

        /**
         * Get the configured deployment timeout.
         *
         * @return timeout
         */
        public long getDeploymentTimeout() {
            long timeOut;
            String time = context.getProperty(DEPLOYMENT_TIMEOUT);
            if (time != null) {
                timeOut = Long.parseLong(time);
            } else {
                timeOut = DEFAULT_DEPLOYMENT_TIMEOUT;
            }
            return timeOut;
        }
    }
}
