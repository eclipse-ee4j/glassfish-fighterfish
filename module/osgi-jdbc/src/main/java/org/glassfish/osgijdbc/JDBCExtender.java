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

package org.glassfish.osgijdbc;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * JDBC extender service.
 */
public final class JDBCExtender implements Extender {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(JDBCExtender.class.getPackage().getName());

    /**
     * The bundle context.
     */
    private final BundleContext bundleContext;

    /**
     * Service registration for the JDBC url handler service.
     */
    private ServiceRegistration urlHandlerService;

    /**
     * Data source factory implementations.
     */
    private final Set<DataSourceFactoryImpl> dataSourceFactories = new HashSet<>();

    /**
     * Bundle tracker.
     */
    private BundleTracker bundleTracker;

    /**
     * Create a new instance.
     *
     * @param context the bundle context
     */
    public JDBCExtender(final BundleContext context) {
        this.bundleContext = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void start() {
        debug("begin start()");
        bundleTracker = new BundleTracker(bundleContext, Bundle.ACTIVE, new JDBCBundleTrackerCustomizer());
        bundleTracker.open();
        addURLHandler();
        debug("completed start()");
    }

    @Override
    public void stop() {
        removeURLHandler();
        if (bundleTracker != null) {
            bundleTracker.close();
        }
        for (DataSourceFactoryImpl dsfi : dataSourceFactories) {
            dsfi.preDestroy();
        }
        debug("stopped");
    }

    /**
     * Get the GlassFish service for the given class.
     *
     * @param <T> service type
     * @param type service class
     * @return T
     */
    private <T> T getService(final Class<T> type) {
        GlassFish gf = (GlassFish) bundleContext.getService(bundleContext.getServiceReference(GlassFish.class.getName()));
        try {
            return gf.getService(type);
        } catch (GlassFishException e) {
            // TODO(Sahoo): Proper Exception Handling
            throw new RuntimeException(e);
        }
    }

    /**
     * Register the URL handler service.
     */
    @SuppressWarnings("unchecked")
    private void addURLHandler() {
        // create parent class-loader (API ClassLoader to access Java EE API)
        ClassLoaderHierarchy clh = getService(ClassLoaderHierarchy.class);
        ClassLoader apiClassLoader = clh.getAPIClassLoader();

        Dictionary p = new Properties();
        p.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] { Constants.JDBC_DRIVER_SCHEME });
        urlHandlerService = bundleContext.registerService(URLStreamHandlerService.class.getName(), new JDBCDriverURLStreamHandlerService(apiClassLoader), p);
    }

    /**
     * Unregister the URL handler service.
     */
    private void removeURLHandler() {
        if (urlHandlerService != null) {
            urlHandlerService.unregister();
            urlHandlerService = null;
        }
    }

    /**
     * Test if the given bundle contains JDBC driver.
     *
     * @param bnd the bundle to test
     * @return {@code true} if the bundle contains some JDBC driver, {@code false} otherwise
     */
    private boolean isJdbcDriverBundle(final Bundle bnd) {
        String osgiRFC = bnd.getHeaders().get(Constants.OSGI_RFC_122);
        return osgiRFC != null && Boolean.valueOf(osgiRFC);
    }

    /**
     * Log a {@code FINE} message.
     *
     * @param msg message to log
     */
    private static void debug(final String msg) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "[osgi-jdbc] : {0}", msg);
        }
    }

    /**
     * Bundle tracker customizer.
     */
    private final class JDBCBundleTrackerCustomizer implements BundleTrackerCustomizer {

        @Override
        @SuppressWarnings("unchecked")
        public Object addingBundle(final Bundle bundle, final BundleEvent event) {

            if (isJdbcDriverBundle(bundle)) {
                debug("Starting JDBC Bundle : " + bundle.getSymbolicName());

                DataSourceFactoryImpl dsfi = new DataSourceFactoryImpl(bundle.getBundleContext());
                dataSourceFactories.add(dsfi);

                Dictionary serviceProperties = new Properties();
                Dictionary header = bundle.getHeaders();
                serviceProperties.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, header.get(Constants.DRIVER.replace(".", "_")));

                String implVersion = (String) header.get(Constants.IMPL_VERSION);
                if (implVersion != null) {
                    serviceProperties.put(DataSourceFactory.OSGI_JDBC_DRIVER_VERSION, implVersion);
                }

                String implTitle = (String) header.get(Constants.IMPL_TITLE);
                if (implTitle != null) {
                    serviceProperties.put(DataSourceFactory.OSGI_JDBC_DRIVER_NAME, implTitle);
                }
                debug(" registering service for driver [" + header.get(Constants.DRIVER.replace(".", "_")) + "]");
                bundle.getBundleContext().registerService(DataSourceFactory.class.getName(), dsfi, serviceProperties);
            }
            // no need to track this any more
            return null;
        }

        @Override
        public void modifiedBundle(final Bundle bundle, final BundleEvent event, final Object object) {
        }

        @Override
        public void removedBundle(final Bundle bundle, final BundleEvent event, final Object object) {
        }
    }
}
