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
package org.glassfish.osgijpa;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.SynchronousBundleListener;

/**
 * An extender that listens for Persistence bundle's life cycle events and takes
 * appropriate actions.
 */
public final class JPAExtender implements Extender, SynchronousBundleListener {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(
            JPAExtender.class.getPackage().getName());

    /**
     * Constant for {@code jpa-extender-state}.
     */
    private static final String PERSISTENT_STATE = "jpa-extender-state";

    /**
     * Property name for enhancer policy.
     */
    private static final String ENHANCER_POLICY_KEY
            = "org.glassfish.osgijpa.enhancerPolicy";

    /**
     * Bundle context.
     */
    private final BundleContext context;

    /**
     * OSGi framework wiring.
     */
    private final FrameworkWiring frameworkWiring;

    /**
     * Bundles to be enhanced.
     */
    private Map<Long, JPABundleProcessor> bundlesToBeEnhanced
            = Collections.synchronizedMap(
                    new HashMap<Long, JPABundleProcessor>());

    /**
     * Executor service used for asynchronous enhancer work.
     */
    private ExecutorService executorService;

    /**
     * Whether enhancement happens in the synchronous bundle listener thread or
     * not. Sometimes, we may run into potential locking issues if we
     * synchronously enhance , as enhancement involves changing bundle state
     * either from INSTALLED to RESOLVED.
     */
    private enum EnhancerPolicy {
        /**
         * Enhancement happens in same thread as bundle listener.
         */
        SYNCHRONOUS,
        /**
         * Enhancement happens in a separate thread than then bundle listener.
         */
        ASYNCHRONOUS
    }

    /**
     * Enhancer policy, default is synchronous.
     */
    private EnhancerPolicy enhancerPolicy = EnhancerPolicy.SYNCHRONOUS;

    /**
     * Create a new instance.
     *
     * @param bndCtx bundle context
     */
    public JPAExtender(final BundleContext bndCtx) {
        this.context = bndCtx;
        this.frameworkWiring = bndCtx.getBundle(0).adapt(
                FrameworkWiring.class);
    }

    @Override
    public void start() {
        String value = context.getProperty(ENHANCER_POLICY_KEY);
        if (value != null) {
            enhancerPolicy = EnhancerPolicy.valueOf(value);
        }
        context.addBundleListener(this);
        executorService = Executors.newSingleThreadExecutor();
        restoreState();
        LOGGER.logp(Level.FINE, "JPAExtender", "start", " JPAExtender started",
                new Object[]{});
    }

    @Override
    public void stop() {
        context.removeBundleListener(this);
        executorService.shutdownNow();
        saveState();
        LOGGER.logp(Level.FINE, "JPAExtender", "stop", " JPAExtender stopped",
                new Object[]{});
    }

    @Override
    @SuppressWarnings("checkstyle:AvoidNestedBlocks")
    public void bundleChanged(final BundleEvent event) {
        final Bundle bundle = event.getBundle();
        switch (event.getType()) {
            case BundleEvent.INSTALLED:
            case BundleEvent.UPDATED: {
                final JPABundleProcessor bi = new JPABundleProcessor(bundle);
                if (!bi.isEnhanced() && bi.isJPABundle()) {
                    LOGGER.logp(Level.INFO, "JPAExtender", "bundleChanged",
                            "Bundle having id {0} is a JPA bundle",
                            new Object[]{bundle.getBundleId()});
                    final Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (tryResolve(bundle)) {
                                // don't refreshPackages. See GLASSFISH-16754
                                // for
                                // kind of ripple effect that can occur because
                                // of refreshPackages even when there are no
                                // other dependencies for a bundle. More over,
                                // since we are enhacing at installation time,
                                // I don't see how any other bundle would have
                                // used our packages unless user installs
                                // bundles using multiple threads.
                                // In such a case, they can always call
                                // refreshPackages themselves after installing
                                // a jpa bundle.
                                enhance(bi, false);
                            } else {
                                LOGGER.log(Level.INFO,
                                        "Bundle having id {0} can't be resolved"
                                        + " now, so adding to a list so that we"
                                        + " can enhance it when it gets"
                                        + " resolved in future",
                                        new Object[]{bundle.getBundleId()});
                                bundlesToBeEnhanced.put(bi.getBundleId(), bi);
                            }
                        }
                    };
                    executeTask(runnable, enhancerPolicy);
                }
                break;
            }
            case BundleEvent.STARTED: {
                long id = bundle.getBundleId();
                final JPABundleProcessor bi = bundlesToBeEnhanced.remove(id);
                if (bi != null) {
                    final Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            // see issue 15189 to know why we pass false
                            enhance(bi, false);
                        }
                    };
                    // Always do it asynchronously since the bundle is already
                    // started.
                    executeTask(runnable, EnhancerPolicy.ASYNCHRONOUS);
                }
                break;
            }
            case BundleEvent.UNINSTALLED: {
                long id = bundle.getBundleId();
                bundlesToBeEnhanced.remove(id);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Enhance the JPA entities.
     * @param bi processor
     * @param refreshPackage flag to indicate if the enhancement should be
     * done deferred to the next framework restart
     */
    private void enhance(final JPABundleProcessor bi,
            final boolean refreshPackage) {
        try {
            Bundle bundle = bi.getBundle();
            InputStream enhancedStream = bi.enhance();
            updateBundle(bundle, enhancedStream);
            if (refreshPackage) {
                frameworkWiring.resolveBundles(
                        Arrays.asList(new Bundle[]{bundle}));
            } else {
                LOGGER.logp(Level.INFO, "JPAExtender", "enhance",
                        "Deferring refresh to framework restart, "
                        + "so enhanced bytes won't come into effect until then"
                        + " for bundle " + bi.getBundleId() + " if there are"
                        + " existing wires to this bundle.");
            }
        } catch (Exception e) {
            LOGGER.logp(Level.WARNING, "JPAExtender", "enhance",
                    "Failed to enhance bundle having id " + bi.getBundleId(),
                    e);
        }
    }

    /**
     * Update the given bundle.
     * @param bundle bundle to update
     * @param enhancedStream input stream
     * @throws BundleException if an error occurs while updating the bundle
     */
    private void updateBundle(final Bundle bundle,
            final InputStream enhancedStream) throws BundleException {

        try {
            bundle.update(enhancedStream);
        } finally {
            try {
                enhancedStream.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Execute the given {@code Runnable} synchronous or asynchronously
     * depending on the configured policy.
     * @param runnable runnable to execute
     * @param policy policy
     */
    private void executeTask(final Runnable runnable,
            final EnhancerPolicy policy) {

        switch (policy) {
            case SYNCHRONOUS:
                runnable.run();
                break;
            case ASYNCHRONOUS:
                executorService.submit(runnable);
            default:
                throw new IllegalArgumentException("Unkown policy");
        }
    }

    /**
     * Resolve the given bundle.
     * @param bundle bundle to resolve
     * @return {@code true} if the bundle is resolved, {@code false} otherwise
     */
    private boolean tryResolve(final Bundle bundle) {
        return frameworkWiring.resolveBundles(
                Arrays.asList(new Bundle[]{bundle}));
    }

    /**
     * Restore the persisted state from disk.
     */
    @SuppressWarnings("unchecked")
    private void restoreState() {
        File baseDir = context.getDataFile("");
        if (baseDir == null) {
            return;
        }
        File state = new File(baseDir, PERSISTENT_STATE);
        if (!state.exists()) {
            return;
        }
        ObjectInputStream stream = null;
        try {
            stream = new ObjectInputStream(new BufferedInputStream(
                    new FileInputStream(state)));
            bundlesToBeEnhanced = (Map<Long, JPABundleProcessor>) stream
                    .readObject();
            LOGGER.logp(Level.INFO, "JPAExtender", "restoreState",
                    "Restored state from {0} and "
                    + "following bundles are yet to be enhanced: {1} ",
                    new Object[]{state.getAbsolutePath(), printBundleIds()});
        } catch (Exception e) {
            LOGGER.logp(Level.WARNING, "JPAExtender", "restoreState",
                    "Unable to read stored data. Will continue with an empty"
                    + " initial state. If you have bundles that were installed"
                    + " earlier and have not been enhanced yet, please update"
                    + " those bundles.", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Persist the current state to disk.
     */
    private void saveState() {
        if (bundlesToBeEnhanced.isEmpty()) {
            return;
        }
        File baseDir = context.getDataFile("");
        if (baseDir == null) {
            return;
        }
        File state = new File(baseDir, PERSISTENT_STATE);
        if (state.exists()) {
            state.delete();
        }
        ObjectOutputStream stream = null;
        try {
            stream = new ObjectOutputStream(new BufferedOutputStream(
                    new FileOutputStream(state)));
            stream.writeObject(bundlesToBeEnhanced);
            LOGGER.logp(Level.INFO, "JPAExtender", "saveState",
                    "Saved state to {0} and "
                    + "following bundles are yet to be enhanced: {1} ",
                    new Object[]{state.getAbsolutePath(), printBundleIds()});
        } catch (Exception e) {
            LOGGER.logp(Level.WARNING, "JPAExtender", "saveState",
                    "Unable to store data. If you have intalled bundles that"
                    + " are yet to be enhanced, they won't be enhanced next"
                    + " time when server starts unless you update those"
                    + " bundles.",
                    e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Create a space separated string of the bundle ids for the bundles to be
     * enhanced.
     * @return StringBuilder
     */
    private StringBuilder printBundleIds() {
        StringBuilder sb = new StringBuilder();
        for (long id : bundlesToBeEnhanced.keySet()) {
            sb.append(id).append(" ");
        }
        return sb;
    }
}
