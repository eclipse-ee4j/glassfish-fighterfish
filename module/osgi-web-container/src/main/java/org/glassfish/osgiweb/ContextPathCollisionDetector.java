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
package org.glassfish.osgiweb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.osgijavaeebase.OSGiApplicationInfo;
import org.glassfish.osgijavaeebase.OSGiContainer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.BundleReference;

import static org.glassfish.osgiweb.Util.getContextPath;

/**
 * Detects collision in Web-ContextPath.
 */
final class ContextPathCollisionDetector implements BundleListener {

    /**
     * This class is an asynchronous bundle listener, because it uses the events
     * for clean up purpose. Since such clean up is considered more of a book
     * keeping rather than essential, we don't want to do it synchronously.
     */
    private static final ContextPathCollisionDetector SELF =
            new ContextPathCollisionDetector();

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(
            ContextPathCollisionDetector.class.getPackage().getName());

    /**
     * What is the currently deployed bundle for a given context path.
     */
    private final Map<String, Long> contextPath2WabMap
            = new HashMap<String, Long>();

    /**
     * What are the colliding WABs for a given context path in addition to the
     * currently deploying/deployed bundle.
     */
    private final Map<String, List<Long>> contextPath2CollidingWabsMap
            = new HashMap<String, List<Long>>();

    /**
     * Service tracker to track the {@code OSGiContainer} service.
     */
    @SuppressWarnings("unchecked")
    private final ServiceTracker osgiContainerTracker = new ServiceTracker(
            getBundle().getBundleContext(), OSGiContainer.class.getName(),
            null);

    /**
     * Flag to indicate if the bundle is stopped.
     */
    private boolean stopped;

    /**
     * Create a new instance.
     */
    private ContextPathCollisionDetector() {
        osgiContainerTracker.open();
        getBundle().getBundleContext().addBundleListener(this);
    }

    /**
     * Get the singleton instance.
     * @return ContextPathCollisionDetector
     */
    public static ContextPathCollisionDetector get() {
        assert (SELF != null);
        return SELF;
    }

    /**
     * Stop the detector. I.e remove the bundle listener.
     */
    synchronized void stop() {
        getBundle().getBundleContext().removeBundleListener(this);
        osgiContainerTracker.close();
        stopped = true;
    }

    /**
     * Check context path collision before a deploy action.
     * @param bundle the bundle to check
     * @throws ContextPathCollisionException if a collision is detected
     */
    @SuppressWarnings("checkstyle:EmptyBlock")
    public synchronized void preDeploy(final Bundle bundle)
            throws ContextPathCollisionException {

        if (stopped) {
            return;
        }
        String contextPath = getContextPath(bundle);
        Long deployedBundle = getCurrentlyDeployedBundle(contextPath);
        final Long bundleId = bundle.getBundleId();
        if (deployedBundle == null) {
            assert (getCollidingWabs(contextPath).isEmpty());
            setCurrentlyDeployedBundle(contextPath, bundleId);
        } else {
            // There are two possibilities, viz:
            // a. we are called from postUndeploy() of this CollisionDetector.
            // In this case, skip any check.
            // b. it's a fresh deploy.
            if (deployedBundle.equals(bundleId)) {
                // case #a
                // This happens when collision detector attempts to deploy a
                // bundle from the colliding WABs list.
                // See postUndeploy() for more details. // Skip any check and
                // return.
            } else {
                // case #b
                addCollidingWab(contextPath, bundleId);
                throw new ContextPathCollisionException(contextPath,
                        getAllWabs(contextPath).toArray(new Long[0]));
            }
        }
    }

    /**
     * Deploy colliding bundle post undeploy of a given bundle.
     * @param bundle the bundle being undeployed
     */
    public synchronized void postUndeploy(final Bundle bundle) {
        if (stopped) {
            return;
        }
        Long bundleId = bundle.getBundleId();
        String contextPath = getContextPath(bundle);
        Long deployedBundle = getCurrentlyDeployedBundle(contextPath);
        assert (bundleId.equals(deployedBundle));
        unsetCurrentlyDeployedBundle(contextPath);
        List<Long> collidingWabs = getCollidingWabs(contextPath);

        // attempt to deploy bundle with lowest bundle id having same context
        // path
        // Although the spec does not require us to attempt to deploy more than
        // the first candidate, we try to deploy
        // other WABs in case the first candidate does not get deployed for
        // whatever reason like it's state has changed, e.g.
        Collections.sort(collidingWabs);
        // use an iterator as we are removing entries
        ListIterator<Long> li = collidingWabs.listIterator();
        while (li.hasNext()) {
            Long nextBundleInList = li.next();
            LOGGER.logp(Level.INFO, "CollisionDetector", "postUndeploy",
                    "Collision detector is attempting to deploy bundle {0}"
                    + " with context path {1} ",
                    new Object[]{nextBundleInList, contextPath});
            try {
                final Bundle nextBundle = getBundle(nextBundleInList);
                // Important protocol:
                // remove it from colliding wab and set it as currently
                // deployed wab
                // By setting it in contextPath2WabMap, we inform the
                // preDeploy() method that it should not detect this as a
                // collision.
                li.remove();
                if (nextBundle == null) {
                    // can happen if bundle has been uninstalled and we have
                    // not managed to clean ourselves up due to inherent
                    // timing issues
                    LOGGER.logp(Level.INFO, "ContextPathCollisionDetector",
                            "postUndeploy",
                            "Collision detector is skipping bundle [{0}], for"
                            + " it has been uninstalled.",
                            new Object[]{nextBundle});
                    continue;
                }
                setCurrentlyDeployedBundle(contextPath, nextBundleInList);
                OSGiApplicationInfo osgiApplicationInfo = getOSGiContainer()
                        .deploy(nextBundle);
                if (osgiApplicationInfo != null) {
                    // break after first successful deploy
                    break;
                }
            } catch (Exception e) {
                // clean up, as we can't rely on cleanUp() being called by
                // container
                unsetCurrentlyDeployedBundle(contextPath);
                LOGGER.logp(Level.WARNING, "CollisionDetector", "postUndeploy",
                        "Collision detector got exception while trying to"
                        + " deploy the bundle with lowest id",
                        e);
            }
        }
    }

    /**
     * Cleanup the given bundle if not stopped.
     * @param bundle the bundle
     */
    public synchronized void cleanUp(final Bundle bundle) {
        if (stopped) {
            return;
        }
        String contextPath = getContextPath(bundle);
        final Long bundleId = bundle.getBundleId();
        final Long deployedBundle = getCurrentlyDeployedBundle(contextPath);
        assert (bundleId.equals(deployedBundle));
        unsetCurrentlyDeployedBundle(contextPath);
        LOGGER.logp(Level.INFO, "CollisionDetector", "cleanUp",
                "Removed bundle {0} against context path {1} ",
                new Object[]{bundleId, contextPath});
    }

    /**
     * Get the bundle currently deployed for the given context path.
     * @param contextPath the context path
     * @return the bundle id or {@code null} if not found
     */
    private synchronized Long getCurrentlyDeployedBundle(
            final String contextPath) {

        return contextPath2WabMap.get(contextPath);
    }

    /**
     * Add the given bundle id to the context map to bundle id map.
     * @param contextPath the context path
     * @param bundleId the bundle id
     */
    private synchronized void setCurrentlyDeployedBundle(
            final String contextPath, final Long bundleId) {

        assert (bundleId != null);
        contextPath2WabMap.put(contextPath, bundleId);
    }

    /**
     * Remove the context path from the context path to bundle id map.
     * @param contextPath context path
     */
    private synchronized void unsetCurrentlyDeployedBundle(
            final String contextPath) {

        contextPath2WabMap.put(contextPath, null);
    }

    /**
     * Get list of colliding bundles with given context path. This method does
     * not return the currently deployed bundle.
     *
     * @param contextPath context path
     * @return list of colliding bundle id for the given context path
     */
    private synchronized List<Long> getCollidingWabs(
            final String contextPath) {

        List<Long> bundleIds = contextPath2CollidingWabsMap.get(contextPath);
        if (bundleIds == null) {
            bundleIds = new ArrayList<Long>();
            contextPath2CollidingWabsMap.put(contextPath, bundleIds);
        }
        return bundleIds;
    }

    /**
     * Add the given bundle to the list of colliding bundles for the given
     * context path.
     * @param contextPath the context path
     * @param bundleId the bundle id
     * @return the updated list of colliding bundle for the context path
     */
    private synchronized List<Long> addCollidingWab(final String contextPath,
            final Long bundleId) {

        final List<Long> bundleIds = getCollidingWabs(contextPath);
        bundleIds.add(bundleId);
        return bundleIds;
    }

    /**
     * Remove the colliding given bundle for the given context path.
     * @param contextPath the context path
     * @param bundleId the id of the bundle to remove
     * @return {@code true} if the bundle was removed, {@code false} otherwise
     */
    private synchronized boolean removeCollingWab(final String contextPath,
            final long bundleId) {

        return getCollidingWabs(contextPath).remove(bundleId);
    }

    /**
     * Get all the web application bundles for the given context path.
     * @param contextPath the context path
     * @return the list of bundle ids
     */
    private synchronized List<Long> getAllWabs(final String contextPath) {
        List<Long> result = new ArrayList<Long>(getCollidingWabs(contextPath));
        final Long deployedBundle = getCurrentlyDeployedBundle(contextPath);
        if (deployedBundle != null) {
            // add it at the beginning
            result.add(0, deployedBundle);
        }
        return result;
    }

    /**
     * Get the bundle with the given id.
     * @param bundleId the bundle id of the bundle to get
     * @return Bundle
     */
    private Bundle getBundle(final Long bundleId) {
        return getBundle().getBundleContext().getBundle(bundleId);
    }

    /**
     * Get the OSGi container service from the tracker.
     * @return OSGiContainer
     */
    private OSGiContainer getOSGiContainer() {
        return (OSGiContainer) osgiContainerTracker.getService();
    }

    /**
     * Get the bundle from the current class-loder.
     * @return Bundle
     */
    private Bundle getBundle() {
        return BundleReference.class.cast(getClass()
                .getClassLoader()).getBundle();
    }

    @Override
    public void bundleChanged(final BundleEvent event) {
        synchronized (this) {
            if (stopped) {
                return;
            }
        }
        if (BundleEvent.STOPPED == event.getType()) {
            Bundle bundle = event.getBundle();
            String contextPath = getContextPath(bundle);
            if (contextPath != null && removeCollingWab(contextPath,
                    bundle.getBundleId())) {
                LOGGER.logp(Level.INFO, "CollisionDetector", "bundleChanged",
                        "Removed bundle [{0}] from colliding bundles list for"
                        + " contextPath {1}",
                        new Object[]{bundle.getBundleId(), contextPath});
            }
        }
    }
}
