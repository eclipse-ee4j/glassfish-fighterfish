/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgiejb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.api.ActionReport;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.osgijavaeebase.AbstractOSGiDeployer;
import org.glassfish.osgijavaeebase.OSGiApplicationInfo;
import org.glassfish.osgijavaeebase.OSGiDeploymentRequest;
import org.glassfish.osgijavaeebase.OSGiUndeploymentRequest;
import org.glassfish.server.ServerEnvironmentImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.Application;

/**
 * Custom deployer for the OSGi EJB container.
 */
public final class OSGiEJBDeployer extends AbstractOSGiDeployer {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(OSGiEJBDeployer.class.getPackage().getName());

    /**
     * Service tracker for the EJB container.
     */
    private final EJBTracker ejbTracker;

    /**
     * JNDI context.
     */
    private final InitialContext ic;

    /**
     * Create a new instance.
     *
     * @param ctx the bundle context
     */
    public OSGiEJBDeployer(final BundleContext ctx) {
        super(ctx, Integer.MIN_VALUE);
        try {
            ic = new InitialContext();
        } catch (NamingException e) {
            // TODO(Sahoo): Proper Exception Handling
            throw new RuntimeException(e);
        }
        ejbTracker = new EJBTracker();
        ejbTracker.open(true);
    }

    @Override
    public OSGiUndeploymentRequest createOSGiUndeploymentRequest(final Deployment deployer, final ServerEnvironmentImpl env, final ActionReport reporter,
            final OSGiApplicationInfo osgiAppInfo) {

        return new OSGiEJBUndeploymentRequest(deployer, env, reporter, osgiAppInfo);
    }

    @Override
    public OSGiDeploymentRequest createOSGiDeploymentRequest(final Deployment deployer, final ArchiveFactory archiveFactory, final ServerEnvironmentImpl env,
            final ActionReport reporter, final Bundle bnd) {

        return new OSGiEJBDeploymentRequest(deployer, archiveFactory, env, reporter, bnd);
    }

    @Override
    public boolean handles(final Bundle bundle) {
        return isEJBBundle(bundle);
    }

    /**
     * Determines if a bundle represents a EJB application or not. We determine this by looking at presence of
     * Application-Type manifest header.
     *
     * @param bnd bundle to test
     * @return {@code true} if the bundle is an EJB bundle, {@code false} otherwise
     */
    private boolean isEJBBundle(final Bundle bnd) {
        final Dictionary headers = bnd.getHeaders();
        return headers.get(Constants.EXPORT_EJB) != null && headers.get(org.osgi.framework.Constants.FRAGMENT_HOST) == null;
    }

    /**
     * An EJBTracker is responsible for registering the desired EJBs in OSGi service registry. It is only applicable for
     * OSGi enabled EJB bundles. Every time such a bundle gets deployed, OSGiContainer registers an
     * {@link org.glassfish.osgijavaeebase.OSGiApplicationInfo}. This class tracks such an object and queries its manifest
     * for {@link Constants#EXPORT_EJB} header. Based on the value of the header, it selects EJBs to be registered as OSGi
     * services. To keep the implementation simple at this point, we only support mapping of stateless EJBs with local
     * business interface views to OSGi services. When an EJB is registered as service, the service properties include the
     * portable JNDI name of the EJB in a service property names {@link #JNDI_NAME_PROP}. All the services are registered
     * under the bundle context of the OSGi/EJB bundle which hosts the EJBs. While registering the EJBs, thread's context
     * class loader is also set to the application class loader of the OSGi/EJB bundle application so that any service
     * tracker (like CDI producer methods) listening to service events will get called in an appropriate context.
     */
    private final class EJBTracker extends ServiceTracker {

        // TODO(Sahoo): More javadoc needed about service properties and service
        // registration
        /**
         * Property name for the JNDI name.
         */
        private static final String JNDI_NAME_PROP = "jndi-name";

        /**
         * Maps bundle id to service registrations.
         */
        // CHECKSTYLE:OFF
        private final Map<Long, Collection<ServiceRegistration>> b2ss = new ConcurrentHashMap<>();
        // CHECKSTYLE:ON

        /**
         * Service registration.
         */
        private ServiceRegistration reg;

        /**
         * Create a new instance.
         */
        @SuppressWarnings("unchecked")
        EJBTracker() {
            super(getBundleContext(), OSGiApplicationInfo.class.getName(), null);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object addingService(final ServiceReference reference) {

            OSGiApplicationInfo osgiApplicationInfo = OSGiApplicationInfo.class.cast(context.getService(reference));
            String exportEJB = osgiApplicationInfo.getBundle().getHeaders().get(Constants.EXPORT_EJB);
            if (exportEJB != null) {
                // remove spaces. I once spent 1 hour trying to debug why EJB
                // was not getting registered
                // and it turned out that user had specified "ALL " in
                // the manifest.
                exportEJB = exportEJB.trim();
                ApplicationInfo ai = osgiApplicationInfo.getAppInfo();
                Application app = ai.getMetaData(Application.class);
                Collection<DolAdapter.EjbDescriptor> ejbs = DolAdapter.convert(app.getEjbDescriptors());
                LOGGER.log(Level.INFO, "addingService: Found {0} no. of EJBs", ejbs.size());
                Collection<DolAdapter.EjbDescriptor> ejbsToBeExported = new ArrayList<>();
                if (Constants.EXPORT_EJB_ALL.equals(exportEJB)) {
                    ejbsToBeExported = ejbs;
                } else if (Constants.EXPORT_EJB_NONE.equals(exportEJB)) {
                    LOGGER.info("addingService: Skipping adding EJBs as OSGi" + " services as per configuration");
                } else {
                    StringTokenizer st = new StringTokenizer(exportEJB, ",");
                    while (st.hasMoreTokens()) {
                        String next = st.nextToken();
                        for (DolAdapter.EjbDescriptor ejb : ejbs) {
                            if (next.equals(ejb.getName())) {
                                ejbsToBeExported.add(ejb);
                            }
                        }
                    }
                }
                b2ss.put(osgiApplicationInfo.getBundle().getBundleId(), new ArrayList<ServiceRegistration>());
                ClassLoader oldTCC = switchTCC(osgiApplicationInfo);
                try {
                    for (DolAdapter.EjbDescriptor ejb : ejbsToBeExported) {
                        registerEjbAsService(ejb, osgiApplicationInfo.getBundle());
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(oldTCC);
                }
            }
            return osgiApplicationInfo;
        }

        /**
         * Switch the thread context class-loader.
         *
         * @param osgiAppInfo application which just got deployed
         * @return the old thread context class-loader
         */
        private ClassLoader switchTCC(final OSGiApplicationInfo osgiAppInfo) {
            ClassLoader newTCC = osgiAppInfo.getClassLoader();
            final Thread thread = Thread.currentThread();
            ClassLoader oldTCC = thread.getContextClassLoader();
            thread.setContextClassLoader(newTCC);
            return oldTCC;
        }

        /**
         * Register the given EJB as a service.
         *
         * @param ejb EJB instance to register
         * @param bundle the application bundle
         */
        @SuppressWarnings("unchecked")
        private void registerEjbAsService(final DolAdapter.EjbDescriptor ejb, final Bundle bundle) {

            System.out.println(ejb);
            try {
                if (com.sun.enterprise.deployment.EjbSessionDescriptor.TYPE.equals(ejb.getType())) {
                    DolAdapter.EjbSessionDescriptor sessionBean = DolAdapter.EjbSessionDescriptor.class.cast(ejb);
                    if (com.sun.enterprise.deployment.EjbSessionDescriptor.STATEFUL.equals(sessionBean.getSessionType())) {
                        LOGGER.warning("Stateful session bean can't be " + "registered as OSGi service");
                    } else {
                        final BundleContext ejbBundleContext = bundle.getBundleContext();
                        for (String lbi : sessionBean.getLocalBusinessClassNames()) {
                            String jndiName = sessionBean.getPortableJndiName(lbi);
                            Object service = null;
                            try {
                                service = ic.lookup(jndiName);
                            } catch (NamingException e) {
                                e.printStackTrace();
                            }
                            Dictionary props = new Properties();
                            props.put(JNDI_NAME_PROP, jndiName);

                            // Note: we register using the bundle context of
                            // the bundle containing the EJB.
                            reg = ejbBundleContext.registerService(lbi, service, props);
                            b2ss.get(bundle.getBundleId()).add(reg);
                        }
                    }
                } else {
                    LOGGER.warning("Only stateless bean or singleton beans " + "can be registered as OSGi service");
                }
            } catch (Exception e) {
                LOGGER.logp(Level.SEVERE, "OSGiEJBDeployer$EJBTracker", "registerEjbAsService", "Exception registering service for ejb by name", e);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void removedService(final ServiceReference reference, final Object service) {

            // when the OSGi-EJB container goes away, the ejb bundle remains in
            // ACTIVE state, so
            // we must unregister the services that OSGi-EJB container has
            // registered on that bundle's behalf.
            OSGiApplicationInfo osgiApplicationInfo = OSGiApplicationInfo.class.cast(service);
            ApplicationInfo ai = osgiApplicationInfo.getAppInfo();
            Application app = ai.getMetaData(Application.class);
            Collection<DolAdapter.EjbDescriptor> ejbs = DolAdapter.convert(app.getEjbDescriptors());
            LOGGER.log(Level.INFO, "removedService: Found {0} no. of EJBs", ejbs.size());
            final Collection<ServiceRegistration> regs = b2ss.get(osgiApplicationInfo.getBundle().getBundleId());
            if (regs != null) {
                // it can be null if this bundle is not an OSGi-EJB bundle.
                for (ServiceRegistration sreg : regs) {
                    if (sreg != null) {
                        try {
                            sreg.unregister();
                        } catch (Exception e) {
                            // If the underlying bundle is stopped, then the
                            // services registered for that context
                            // would have already been unregistered, so an
                            // IllegalStateException can be raised here.
                            // log it in FINE level and ignore.
                            LOGGER.logp(Level.FINE, "OSGiEJBDeployer$EJBTracker", "removedService", "Exception unregistering " + sreg, e);
                        }
                    }
                }
            }
            super.removedService(reference, service);
        }
    }
}
