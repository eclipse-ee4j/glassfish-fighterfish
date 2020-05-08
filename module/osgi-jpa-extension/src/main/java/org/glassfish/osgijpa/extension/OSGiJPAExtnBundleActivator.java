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
package org.glassfish.osgijpa.extension;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import javax.persistence.spi.PersistenceProviderResolver;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This activator is responsible for setting persistence provider resolver that enables discovery of providers even if
 * thread's context class loader is not properly set.
 */
public final class OSGiJPAExtnBundleActivator implements BundleActivator {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(OSGiJPAExtnBundleActivator.class.getPackage().getName());

    /**
     * Property for enabling the hybrid provider resolver.
     */
    // CHECKSTYLE:OFF
    private static final String USE_OSGI_PROVIDER_RESOLVER = "org.glassfish.osgjpa.extension.useHybridPersistenceProviderResolver";
    // CHECKSTYLE:ON

    /**
     * Property for enabling the caching on the provider resolver.
     */
    // CHECKSTYLE:OFF
    private static final String OSGI_PROVIDER_RESOLVER_CACHING_ENABLED = "org.glassfish.osgjpa.extension.hybridPersistenceProviderResolver.cachingEnabled";
    // CHECKSTYLE:ON

    @Override
    public void start(final BundleContext context) throws Exception {
        boolean useOSGiProviderResolver = Boolean.parseBoolean(context.getProperty(USE_OSGI_PROVIDER_RESOLVER));
        if (useOSGiProviderResolver) {
            boolean cachingEnabled = Boolean.parseBoolean(context.getProperty(OSGI_PROVIDER_RESOLVER_CACHING_ENABLED));
            final PersistenceProviderResolver oldResolver = PersistenceProviderResolverHolder.getPersistenceProviderResolver();
            final PersistenceProviderResolver newResolver = new HybridPersistenceProviderResolver(cachingEnabled);
            PersistenceProviderResolverHolder.setPersistenceProviderResolver(newResolver);
            LOGGER.logp(Level.FINE, "OSGiJPAExtnBundleActivator", "start", "Old resolver = {0}, New Reoslver = {1} ",
                    new Object[] { oldResolver, newResolver });
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        PersistenceProviderResolverHolder.setPersistenceProviderResolver(null);
    }
}
