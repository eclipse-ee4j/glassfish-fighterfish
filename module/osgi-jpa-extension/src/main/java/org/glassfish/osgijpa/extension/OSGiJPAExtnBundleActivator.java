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

import static java.util.logging.Level.FINE;

import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import jakarta.persistence.spi.PersistenceProviderResolver;
import jakarta.persistence.spi.PersistenceProviderResolverHolder;

/**
 * This activator is responsible for setting persistence provider resolver that enables discovery of providers even if
 * thread's context class loader is not properly set.
 */
public final class OSGiJPAExtnBundleActivator implements BundleActivator {

    private static final Logger LOGGER = Logger.getLogger(OSGiJPAExtnBundleActivator.class.getPackage().getName());

    /**
     * Property for enabling the hybrid provider resolver.
     */
    private static final String USE_OSGI_PROVIDER_RESOLVER = "org.glassfish.osgjpa.extension.useHybridPersistenceProviderResolver";

    /**
     * Property for enabling the caching on the provider resolver.
     */
    private static final String OSGI_PROVIDER_RESOLVER_CACHING_ENABLED = "org.glassfish.osgjpa.extension.hybridPersistenceProviderResolver.cachingEnabled";

    @Override
    public void start(BundleContext context) throws Exception {
        boolean useOSGiProviderResolver = Boolean.parseBoolean(context.getProperty(USE_OSGI_PROVIDER_RESOLVER));
        
        if (useOSGiProviderResolver) {
            boolean cachingEnabled = Boolean.parseBoolean(context.getProperty(OSGI_PROVIDER_RESOLVER_CACHING_ENABLED));
            PersistenceProviderResolver oldResolver = PersistenceProviderResolverHolder.getPersistenceProviderResolver();
            PersistenceProviderResolver newResolver = new HybridPersistenceProviderResolver(cachingEnabled);
            
            PersistenceProviderResolverHolder.setPersistenceProviderResolver(newResolver);
            
            LOGGER.logp(FINE, "OSGiJPAExtnBundleActivator", "start", "Old resolver = {0}, New resolver = {1} ",
                    new Object[] { oldResolver, newResolver });
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        PersistenceProviderResolverHolder.setPersistenceProviderResolver(null);
    }
}
