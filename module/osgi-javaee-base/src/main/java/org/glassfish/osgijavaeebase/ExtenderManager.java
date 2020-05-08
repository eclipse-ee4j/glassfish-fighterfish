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

import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * It is responsible for starting any registered {@link Extender} service after GlassFish server is started and stopping
 * them when server is shutdown. We use GlassFish STARTED event to be notified of server startup and shutdown.
 */
class ExtenderManager {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ExtenderManager.class.getPackage().getName());

    /**
     * The bundle context.
     */
    private final BundleContext context;

    /**
     * GlassFish life-cycle events.
     */
    private Events events;

    /**
     * GlassFish life-cycle event listener.
     */
    private EventListener listener;

    /**
     * Service tracker for the extender service.
     */
    private ServiceTracker extenderTracker;

    /**
     * Service tracker for the GlassFish service.
     */
    private GlassFishServerTracker glassFishServerTracker;

    /**
     * Create a new instance.
     * 
     * @param ctx the bundle context
     */
    ExtenderManager(final BundleContext ctx) {
        this.context = ctx;
    }

    /**
     * Start the extender manager.
     * 
     * @throws Exception if an error occurs
     */
    public final synchronized void start() throws Exception {
        LOGGER.logp(Level.FINE, "ExtenderManager", "start", "ExtenderManager starting");
        glassFishServerTracker = new GlassFishServerTracker(context);
        glassFishServerTracker.open();
    }

    /**
     * Stop the extender manager.
     * 
     * @throws Exception if an error occurs
     */
    public final synchronized void stop() throws Exception {
        LOGGER.logp(Level.FINE, "ExtenderManager", "start", "ExtenderManager stopping");
        unregisterGlassFishShutdownHook();
        if (glassFishServerTracker != null) {
            glassFishServerTracker.close();
            glassFishServerTracker = null;
        }
        stopExtenders();
    }

    /**
     * Start the registered extenders.
     */
    private synchronized void startExtenders() {
        LOGGER.entering("ExtenderManager", "startExtenders");

        // Because of a race condition, we can be started multiple times,
        // so check if already started
        if (extenderTracker != null) {
            return;
        }

        // open will call addingService for each existing extender
        // and there by we will start each extender.
        extenderTracker = new ExtenderTracker(context);
        extenderTracker.open();
    }

    /**
     * Stop the registered extenders.
     */
    private synchronized void stopExtenders() {
        LOGGER.entering("ExtenderManager", "stopExtenders");

        // Because of a race condition, we can be stopped multiple times,
        // so check if already started
        // more over, extenderTracker will be null until server is started,
        // so to avoid NPE, null check is needed.
        if (extenderTracker == null) {
            return;
        }

        // close will call removedService for each tracked extender
        // and there by we will stop each extender.
        extenderTracker.close();
        extenderTracker = null;
    }

    /**
     * Unregister the GlassFish life-cycle event listener.
     */
    private void unregisterGlassFishShutdownHook() {
        if (listener != null) {
            events.unregister(listener);
        }
    }

    /**
     * OSGi service tracker for {@link Extender} service.
     */
    private class ExtenderTracker extends ServiceTracker {

        /**
         * Create a new instance.
         * 
         * @param ctx the bundle context
         */
        @SuppressWarnings("unchecked")
        ExtenderTracker(final BundleContext ctx) {
            super(ctx, Extender.class.getName(), null);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object addingService(final ServiceReference reference) {
            Extender e = Extender.class.cast(context.getService(reference));
            LOGGER.logp(Level.FINE, "ExtenderManager$ExtenderTracker", "addingService", "Starting extender called {0}", new Object[] { e });
            e.start();
            return e;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void removedService(final ServiceReference reference, final Object service) {

            Extender e = Extender.class.cast(context.getService(reference));
            LOGGER.logp(Level.FINE, "ExtenderManager$ExtenderTracker", "removedService", "Stopping extender called {0}", new Object[] { e });
            e.stop();
        }
    }

    /**
     * Tracks GlassFish and obtains EVents service from it and registers a listener that takes care of actually starting and
     * stopping other extenders.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    private class GlassFishServerTracker extends ServiceTracker {

        /**
         * Create a new instance.
         * 
         * @param ctx the bundle context
         */
        @SuppressWarnings("unchecked")
        GlassFishServerTracker(final BundleContext ctx) {
            super(ctx, GlassFish.class.getName(), null);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object addingService(final ServiceReference reference) {
            LOGGER.logp(Level.FINE, "ExtenderManager$GlassFishServerTracker", "addingService", "GlassFish has been created");
            final GlassFish gf = GlassFish.class.cast(context.getService(reference));
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            return executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Poll for GlassFish to start. GlassFish service might
                        // have been registered by
                        // GlassFishRuntime.newGlassFish() and hence might not
                        // be ready to use.
                        // This is the case for GlassFish < 4.0
                        GlassFish.Status status;
                        while ((status = gf.getStatus()) != GlassFish.Status.STARTED) {
                            if (status == GlassFish.Status.DISPOSED) {
                                return;
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                        // start extenders first before registering for events,
                        // otherwise we can deadlock
                        // if startExtender() is in progress and glassfish sends
                        // PREPARE_SHUTDOWN event.
                        startExtenders();
                        events = gf.getService(Events.class);
                        listener = new EventListener() {
                            @Override
                            public void event(final Event event) {
                                if (EventTypes.PREPARE_SHUTDOWN.equals(event.type())) {
                                    stopExtenders();
                                }
                            }
                        };
                        events.register(listener);
                    } catch (GlassFishException e) {
                        // TODO(Sahoo): Proper Exception Handling
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        @Override
        @SuppressWarnings("unchecked")
        public void removedService(final ServiceReference reference, final Object service) {

            Future future = (Future) service;
            // interrupt if it is still waiting for gf to start or stop
            future.cancel(true);
            super.removedService(reference, service);
        }
    }
}
