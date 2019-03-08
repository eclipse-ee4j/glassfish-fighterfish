/*
 * Copyright (c) 2010, 2019 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.hk2.osgiresourcelocator.ServiceLoader;

import javax.persistence.spi.PersistenceProvider;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a custom implementation of
 * {@link javax.persistence.spi.PersistenceProviderResolver} which has the
 * ability to discover providers that are part of OSGi bundles. As you know, a
 * thread's context loader (TCL) is very widely assumed to represent what an
 * application can see. This is no different in JPA as well. However, there
 * exists some technologies like OSGi, which do not want to rely on Thread's
 * context loader to denote visibility scope of an application. In order to
 * accommodate these diverse technologies, it first attempts to discover
 * providers using TCL. If some providers are visible to the TCL, it considers
 * them as the only set of providers that are supposed to be visible to the
 * current thread and hence returns that list. If TCL can't find any providers,
 * then it assumes that it is invoked in a context which is not relying on TCL
 * to limit visibility, hence it goes onto discover all providers installed in
 * the current framework. To discover providers installed by OSGi bundles, it
 * currently relies on provider bundles to have
 * META-INF/services/javax.persistence.PersistenceProvider file as every JPA
 * compliant provider bundles such a resource. In future, we can even enhance
 * this class to discover PersistenceProvider service registered by OSGi/JPA
 * compliant bundles.
 * <p/>
 * As per the requirement of JPA spec, this implementation is thread-safe.
 * Please note, this class comes into picture even in Java EE mode usage of JPA
 * via {@link javax.persistence.spi.PersistenceProvider#getProviderUtil()},
 * which is the only way to call methods like
 * {@link javax.persistence.spi.ProviderUtil#isLoaded(Object)}. So, it is
 * important for this class to be performant. So, this class also supports
 * caching mode which can be explicitly enabled at construction time. Caching
 * should only be enabled in environment where providers are not
 * installed/uninstalled dynamically.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class HybridPersistenceProviderResolver implements
        javax.persistence.spi.PersistenceProviderResolver {

    // Yes, I am fully aware that eclipselink produced javax.persistence bundle
    // also has an activator
    // which has code to set a custom resolver, but that resolver only works for
    // RFC#143 compliant OSGi JPA providers.
    // So, that does not work for us in GlassFish, as we are really a hybrid
    // environment and our users primarily
    // use JPA in non-OSGi mode. SO, we need this resolver which caters to the
    // need of both kinds of users.
    // Should we hold a WeakReference to provider instead of name of the
    // provider?
    private final Map<ClassLoader, List<String>> cl2ProviderNames
            = Collections.synchronizedMap(
                    new WeakHashMap<ClassLoader, List<String>>());

    private final boolean cachingEnabled;

    private static final Logger LOGGER = Logger.getLogger(
            HybridPersistenceProviderResolver.class.getPackage().getName());

    public HybridPersistenceProviderResolver(boolean cachingEnabled) {
        LOGGER.logp(Level.FINE, "HybridPersistenceProviderResolver",
                "HybridPersistenceProviderResolver",
                "cachingEnabled = {0}", new Object[]{cachingEnabled});
        this.cachingEnabled = cachingEnabled;
    }

    @Override
    public List<PersistenceProvider> getPersistenceProviders() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return getPersistenceProviders(cl);
    }

    private List<PersistenceProvider> getPersistenceProviders(ClassLoader cl) {
        List<PersistenceProvider> providers;
        if (isCachingEnabled()) {
            providers = readCache(cl);
            if (providers == null) {
                providers = discoverPersistenceProviders(cl);
                populateCache(cl, providers);
            }
        } else {
            providers = discoverPersistenceProviders(cl);
        }
        return providers;
    }

    private void populateCache(ClassLoader cl,
            List<PersistenceProvider> providers) {

        List<String> providerNames = new ArrayList<String>(providers.size());
        providerNames.addAll(convert(providers));
        cl2ProviderNames.put(cl, providerNames);
    }

    private List<PersistenceProvider> readCache(ClassLoader cl) {
        List<String> providerNames = cl2ProviderNames.get(cl);
        return providerNames != null ? convert(providerNames, cl) : null;
    }

    /**
     * Converts a list of provider objects to a list of class names where each
     * name corresponds to provider class name.
     *
     * @param providers list of providers
     * @return list of class names
     */
    private List<String> convert(Iterable<PersistenceProvider> providers) {
        List<String> result = new ArrayList<String>();
        for (PersistenceProvider p : providers) {
            result.add(p.getClass().getName());
        }
        return result;
    }

    /**
     * For each name passed in the list, it tries to load the class using the
     * supplied loader, instantiate it and adds it to the list of result object
     * before returning it. If for some reason, the cached provider is no longer
     * available, then it just logs a warning and continues. Since we discover
     * the providers using a single class loader in
     * {@link #discoverPersistenceProviders(ClassLoader)} (ClassLoader)} method,
     * we expect all of them to be loadable using the same loader as well.
     *
     * @param providerNames list of provider class names.
     * @param cl class loader to be used to load provider classes
     * @return list of provider objects.
     */
    private List<PersistenceProvider> convert(Iterable<String> providerNames,
            ClassLoader cl) {

        List<PersistenceProvider> result = new ArrayList<PersistenceProvider>();
        for (String name : providerNames) {
            try {
                result.add((PersistenceProvider) cl.loadClass(name).newInstance());
            } catch (Exception e) {
                LOGGER.logp(Level.WARNING, "HybridPersistenceProviderResolver",
                        "convert",
                        "Exception trying to instantiate cached provider by name "
                        + name, e);
            }
        }
        return result;
    }

    private List<PersistenceProvider> discoverPersistenceProviders(
            ClassLoader cl) {

        List<PersistenceProvider> result
                = new ArrayList<PersistenceProvider>();
        if (cl != null) {
            Iterator<PersistenceProvider> services = java.util.ServiceLoader
                    .load(PersistenceProvider.class, cl).iterator();
            while (services.hasNext()) {
                try {
                    PersistenceProvider p = services.next();
                    result.add(p);
                } catch (ServiceConfigurationError e) {
                    // can happen if a cached provider has been uninstalled or
                    // something of that sort.
                    LOGGER.logp(Level.FINE, "HybridPersistenceProviderResolver",
                            "getPersistenceProviders",
                            "Exception while discovering providers for class loader "
                            + cl, e);
                }
            }
        }
        if (result.isEmpty()) {
            // Ok, we are called in a context where TCL can't see any provider
            // . e.g.,
            // when an OSGi bundle's activator uses Persistence class. So,
            // discover all providers installed in the framework.
            result.addAll(discoverOSGiProviders());
        }
        return result;
    }

    /**
     * @return persistence providers made available by OSGi bundles installed in
     * the current framework.
     */
    private List<PersistenceProvider> discoverOSGiProviders() {
        List<PersistenceProvider> result
                = new ArrayList<PersistenceProvider>();
        for (PersistenceProvider p : ServiceLoader
                .lookupProviderInstances(PersistenceProvider.class)) {
            result.add(p);
        }
        return result;
    }

    @Override
    public synchronized void clearCachedProviders() {
        if (isCachingEnabled()) {
            cl2ProviderNames.clear();
        }
    }

    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    @Override
    public String toString() {
        return super.toString() + "[CachingEnabled = " + isCachingEnabled()
                + "]";
    }
}
