/*
 * Copyright (c) 2009, 2018 Oracle and/or its affiliates. All rights reserved.
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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This extender is responsible for detecting and deploying any Java EE OSGi
 * bundle. Implementation Note: All methods are synchronized, because we don't
 * allow the extender to stop while it is deploying or undeploying something.
 * Similarly, while it is being stopped, we don't want it to deploy or undeploy
 * something. After receiving the event, it spwans a separate thread to carry
 * out the task so that we don't spend long time in the synchronous event
 * listener. More over, that can lead to deadlocks as observed in
 * https://glassfish.dev.java.net/issues/show_bug.cgi?id=14313.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class JavaEEExtender implements Extender {

    private static final String DEPLOYMENT_TIMEOUT
            = "org.glassfish.osgijavaeebase.deployment.timeout";
    private volatile OSGiContainer c;
    private static final Logger LOGGER = Logger.getLogger(
            JavaEEExtender.class.getPackage().getName());

    private final BundleContext context;
    private ServiceRegistration reg;
    private BundleTracker tracker;
    private ExecutorService executorService;

    public JavaEEExtender(BundleContext context) {
        this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void start() {
        executorService = Executors.newSingleThreadExecutor();
        c = new OSGiContainer(context);
        c.init();
        reg = context.registerService(OSGiContainer.class.getName(), c, null);
        tracker = new BundleTracker(context, Bundle.ACTIVE | Bundle.STARTING,
                new HybridBundleTrackerCustomizer());
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

    private synchronized OSGiApplicationInfo deploy(Bundle b) {
        if (!isStarted()) {
            return null;
        }
        try {
            return c.deploy(b);
        } catch (Throwable e) {
            LOGGER.logp(Level.SEVERE, "JavaEEExtender", "deploy",
                    "Exception deploying bundle {0}",
                    new Object[]{b.getLocation()});
            LOGGER.logp(Level.SEVERE, "JavaEEExtender", "deploy",
                    "Exception Stack Trace", e);
        }
        return null;
    }

    private synchronized void undeploy(Bundle b) {
        if (!isStarted()) {
            return;
        }
        try {
            if (c.isDeployed(b)) {
                c.undeploy(b);
            }
        } catch (Exception e) {
            LOGGER.logp(Level.SEVERE, "JavaEEExtender", "undeploy",
                    "Exception undeploying bundle {0}",
                    new Object[]{b.getLocation()});
            LOGGER.logp(Level.SEVERE, "JavaEEExtender", "undeploy",
                    "Exception Stack Trace", e);
        }
    }

    private boolean isStarted() {
        // This method is deliberately made non-synchronized, because it is
        // called from tracker customizer
        return c != null;
    }

    private class HybridBundleTrackerCustomizer
            implements BundleTrackerCustomizer {

        private final Map<Long, Future<OSGiApplicationInfo>> deploymentTasks
                = new ConcurrentHashMap<Long, Future<OSGiApplicationInfo>>();

        @Override
        public Object addingBundle(final Bundle bundle, BundleEvent event) {
            if (!isStarted()) {
                return null;
            }
            final int state = bundle.getState();
            if (isReady(event, state)) {
                Future<OSGiApplicationInfo> future = executorService
                        .submit(new Callable<OSGiApplicationInfo>() {
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
         * Bundle is ready when its state is ACTIVE or, when a lazy activation
         * policy is used, STARTING
         *
         * @param event
         * @param state
         * @return
         */
        private boolean isReady(BundleEvent event, int state) {
            return state == Bundle.ACTIVE
                    || (state == Bundle.STARTING
                    && (event != null
                    && event.getType() == BundleEvent.LAZY_ACTIVATION));
        }

        @Override
        public void modifiedBundle(Bundle bundle, BundleEvent event,
                Object object) {
        }

        @Override
        public void removedBundle(final Bundle bundle, BundleEvent event,
                Object object) {

            if (!isStarted()) {
                return;
            }
            Future<OSGiApplicationInfo> deploymentTask = deploymentTasks
                    .remove(bundle.getBundleId());
            if (deploymentTask == null) {
                // We have never seen this bundle before. Ideally we should
                // never get here.
                assert (false);
                return;
            }
            try {
                OSGiApplicationInfo deployedApp = null;
                try {
                    deployedApp = deploymentTask.get(getDeploymentTimeout(),
                            TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    LOGGER.logp(Level.FINE,
                            "JavaEEExtender$HybridBundleTrackerCustomizer",
                            "removedBundle",
                            "Undeployer times out waiting for deployment to finish for bundle "
                                    + bundle, e);
                    boolean isCancelled = deploymentTask.cancel(true);
                    if (!isCancelled) {
                        // cancellation of timer won't be successful if the
                        // deployer has finished by the time we attempt to
                        // cancel
                        deployedApp = deploymentTask.get();
                    } else {
                        LOGGER.logp(Level.INFO,
                                "JavaEEExtender$HybridBundleTrackerCustomizer",
                                "removedBundle", "isCancelled = {0}",
                                new Object[]{isCancelled});
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
                LOGGER.logp(Level.FINE,
                        "JavaEEExtender$HybridBundleTrackerCustomizer",
                        "removedBundle", "e = {0}", new Object[]{e});
            }
        }

        public long getDeploymentTimeout() {
            long timeOut;
            String time = context.getProperty(DEPLOYMENT_TIMEOUT);
            if (time != null) {
                timeOut = Long.valueOf(time);
            } else {
                timeOut = 10000;
            }
            return timeOut;
        }
    }
}
