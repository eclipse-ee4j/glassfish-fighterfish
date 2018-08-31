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

package org.glassfish.osgijdbc;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.*;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JDBCExtender implements Extender {

    private BundleContext bundleContext;

    private ServiceRegistration urlHandlerService;

    private Set<DataSourceFactoryImpl> dataSourceFactories = new HashSet<DataSourceFactoryImpl>();

    private BundleTracker bundleTracker;

    private static final Logger logger = Logger.getLogger(
            JDBCExtender.class.getPackage().getName());

    public JDBCExtender(BundleContext context) {
        this.bundleContext = context;
    }

    public void start() {
        debug("begin start()");
        bundleTracker = new BundleTracker(bundleContext, Bundle.ACTIVE, new JDBCBundleTrackerCustomizer());
        bundleTracker.open();
        addURLHandler();
        debug("completed start()");
    }

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

    private <T> T getService(Class<T> type){
        GlassFish gf = (GlassFish) bundleContext.getService(bundleContext.getServiceReference(GlassFish.class.getName()));
        try {
            return gf.getService(type);
        } catch (GlassFishException e) {
            throw new RuntimeException(e); // TODO(Sahoo): Proper Exception Handling
        }
    }

    private void addURLHandler() {

        //create parent class-loader (API ClassLoader to access Java EE API)
        ClassLoaderHierarchy clh = getService(ClassLoaderHierarchy.class);
        ClassLoader apiClassLoader = clh.getAPIClassLoader();

        Properties p = new Properties();
        p.put(URLConstants.URL_HANDLER_PROTOCOL, new String[]{Constants.JDBC_DRIVER_SCHEME});
        urlHandlerService = bundleContext.registerService(URLStreamHandlerService.class.getName(),
                new JDBCDriverURLStreamHandlerService(apiClassLoader), p);
    }

    private void removeURLHandler() {
        if (urlHandlerService != null) {
            urlHandlerService.unregister();
            urlHandlerService = null;
        }
    }

    private boolean isJdbcDriverBundle(Bundle b) {
        String osgiRFC = (String) b.getHeaders().get(Constants.OSGI_RFC_122);
        if (osgiRFC != null && Boolean.valueOf(osgiRFC)) {
            return true;
        } else {
            return false;
        }
    }

    private void debug(String s) {
        if(logger.isLoggable(Level.FINEST)){
            logger.finest("[osgi-jdbc] : " + s);
        }
    }

    private class JDBCBundleTrackerCustomizer implements BundleTrackerCustomizer {
        public Object addingBundle(Bundle bundle, BundleEvent event) {
            if (isJdbcDriverBundle(bundle)) {
                debug("Starting JDBC Bundle : " + bundle.getSymbolicName());

                DataSourceFactoryImpl dsfi = new DataSourceFactoryImpl(bundle.getBundleContext());
                dataSourceFactories.add(dsfi);

                Properties serviceProperties = new Properties();
                Dictionary header = bundle.getHeaders();
                serviceProperties.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS,
                        header.get(Constants.DRIVER.replace(".", "_")));

                String implVersion = (String) header.get(Constants.IMPL_VERSION);
                if (implVersion != null) {
                    serviceProperties.put(DataSourceFactory.OSGI_JDBC_DRIVER_VERSION, implVersion);
                }

                String implTitle = (String) header.get(Constants.IMPL_TITLE);
                if (implTitle != null) {
                    serviceProperties.put(DataSourceFactory.OSGI_JDBC_DRIVER_NAME, implTitle);
                }
                debug(" registering service for driver [" +
                        header.get(Constants.DRIVER.replace(".", "_")) + "]");
                bundle.getBundleContext().registerService(DataSourceFactory.class.getName(),
                        dsfi, serviceProperties);
            }
            return null; // no need to track this any more
        }

        public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        }

        public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        }
    }
}
