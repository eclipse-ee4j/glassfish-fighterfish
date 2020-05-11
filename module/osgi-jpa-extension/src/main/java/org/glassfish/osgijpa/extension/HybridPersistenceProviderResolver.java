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

import static java.lang.Thread.currentThread;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.glassfish.hk2.osgiresourcelocator.ServiceLoader;

import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceProviderResolver;

/**
 * This is a custom implementation of {@link jakarta.persistence.spi.PersistenceProviderResolver} which has the ability to
 * discover providers that are part of OSGi bundles. 
 * 
 * <p>
 * As you know, a thread's context loader (TCL) is very widely assumed
 * to represent what an application can see. This is no different in JPA as well. However, there exists some
 * technologies like OSGi, which do not want to rely on Thread's context loader to denote visibility scope of an
 * application. In order to accommodate these diverse technologies, it first attempts to discover providers using TCL.
 * If some providers are visible to the TCL, it considers them as the only set of providers that are supposed to be
 * visible to the current thread and hence returns that list. If TCL can't find any providers, then it assumes that it
 * is invoked in a context which is not relying on TCL to limit visibility, hence it goes onto discover all providers
 * installed in the current framework. To discover providers installed by OSGi bundles, it currently relies on provider
 * bundles to have META-INF/services/jakarta.persistence.PersistenceProvider file as every JPA compliant provider bundles
 * such a resource. In future, we can even enhance this class to discover PersistenceProvider service registered by
 * OSGi/JPA compliant bundles.
 * 
 * <p/>
 * As per the requirement of JPA spec, this implementation is thread-safe. Please note, this class comes into picture
 * even in Java EE mode usage of JPA via {@link jakarta.persistence.spi.PersistenceProvider#getProviderUtil()}, which is
 * the only way to call methods like {@link jakarta.persistence.spi.ProviderUtil#isLoaded(Object)}. So, it is important
 * for this class to be performant. So, this class also supports caching mode which can be explicitly enabled at
 * construction time. Caching should only be enabled in environment where providers are not installed/uninstalled
 * dynamically.
 */
public final class HybridPersistenceProviderResolver implements PersistenceProviderResolver {

    private static final Logger LOGGER = Logger.getLogger(HybridPersistenceProviderResolver.class.getPackage().getName());

    // Yes, I am fully aware that eclipselink produced jakarta.persistence bundle
    // also has an activator
    // which has code to set a custom resolver, but that resolver only works for
    // RFC#143 compliant OSGi JPA providers.
    // So, that does not work for us in GlassFish, as we are really a hybrid
    // environment and our users primarily
    // use JPA in non-OSGi mode. SO, we need this resolver which caters to the
    // need of both kinds of users.
    // Should we hold a WeakReference to provider instead of name of the
    // provider?
    /**
     * Map of class loader to provider names.
     */
    private final Map<ClassLoader, List<String>> cl2ProviderNames = Collections.synchronizedMap(new WeakHashMap<ClassLoader, List<String>>());

    /**
     * Flag to indicate if caching is enabled.
     */
    private final boolean cachingEnabled;

    /**
     * Create a new instance.
     *
     * @param caching caching enabled flag
     */
    public HybridPersistenceProviderResolver(final boolean caching) {
        LOGGER.logp(FINE, "HybridPersistenceProviderResolver", "HybridPersistenceProviderResolver", "cachingEnabled = {0}", new Object[] { caching });
        this.cachingEnabled = caching;
    }

    @Override
    public List<PersistenceProvider> getPersistenceProviders() {
        return getPersistenceProviders(currentThread().getContextClassLoader());
    }

    /**
     * Get the persistence providers for the given class-loader.
     *
     * @param cl class-loader
     * @return list of persistence providers
     */
    private List<PersistenceProvider> getPersistenceProviders(final ClassLoader cl) {
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

    /**
     * Populate the cache.
     *
     * @param classLoader class-loader
     * @param providers list of providers
     */
    private void populateCache(ClassLoader classLoader, List<PersistenceProvider> providers) {
        List<String> providerNames = new ArrayList<>(providers.size());
        providerNames.addAll(convert(providers));
        cl2ProviderNames.put(classLoader, providerNames);
    }

    /**
     * Read the providers from the cache for a given class-loader.
     *
     * @param classLoader class-loader
     * @return list of providers
     */
    private List<PersistenceProvider> readCache(ClassLoader classLoader) {
        List<String> providerNames = cl2ProviderNames.get(classLoader);
        if (providerNames != null) {
            return convert(providerNames, classLoader);
        }
        
        return null;
    }

    /**
     * Converts a list of provider objects to a list of class names where each name corresponds to provider class name.
     *
     * @param providers list of providers
     * @return list of class names
     */
    private List<String> convert(final Iterable<PersistenceProvider> providers) {
        List<String> result = new ArrayList<>();
        for (PersistenceProvider provider : providers) {
            result.add(provider.getClass().getName());
        }
        
        return result;
    }

    /**
     * For each name passed in the list, it tries to load the class using the supplied loader, instantiate it and adds it to
     * the list of result object before returning it. If for some reason, the cached provider is no longer available, then
     * it just logs a warning and continues. Since we discover the providers using a single class loader in
     * {@link #discoverPersistenceProviders(ClassLoader)} (ClassLoader)} method, we expect all of them to be loadable using
     * the same loader as well.
     *
     * @param providerNames list of provider class names.
     * @param classLoader class loader to be used to load provider classes
     * @return list of provider objects.
     */
    private List<PersistenceProvider> convert(Iterable<String> providerNames, ClassLoader classLoader) {
        List<PersistenceProvider> result = new ArrayList<>();
        for (String name : providerNames) {
            try {
                result.add((PersistenceProvider) classLoader.loadClass(name).newInstance());
            } catch (Exception e) {
                LOGGER.logp(WARNING, "HybridPersistenceProviderResolver", "convert",
                        "Exception trying to instantiate cached provider by" + " name " + name, e);
            }
        }
        
        return result;
    }

    /**
     * Discover the providers in the given class-loader.
     *
     * @param classLoader class-loader
     * @return discovered list of providers
     */
    private List<PersistenceProvider> discoverPersistenceProviders(ClassLoader classLoader) {
        List<PersistenceProvider> result = new ArrayList<>();
        
        if (classLoader != null) {
            Iterator<PersistenceProvider> services = java.util.ServiceLoader.load(PersistenceProvider.class, classLoader).iterator();
            while (services.hasNext()) {
                try {
                    result.add(services.next());
                } catch (ServiceConfigurationError e) {
                    // can happen if a cached provider has been uninstalled or
                    // something of that sort.
                    LOGGER.logp(FINE, "HybridPersistenceProviderResolver", "getPersistenceProviders",
                            "Exception while discovering providers for class" + " loader " + classLoader, e);
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
     * Discover the providers made available by OSGi bundles installed in the current framework.
     *
     * @return list of discovered providers
     */
    private List<PersistenceProvider> discoverOSGiProviders() {
        List<PersistenceProvider> result = new ArrayList<>();
        for (PersistenceProvider provider : ServiceLoader.lookupProviderInstances(PersistenceProvider.class)) {
            result.add(provider);
        }
        
        return result;
    }

    @Override
    public synchronized void clearCachedProviders() {
        if (isCachingEnabled()) {
            cl2ProviderNames.clear();
        }
    }

    /**
     * Test if caching is enabled.
     *
     * @return {@code true} if caching is enabled, {@code false} otherwise
     */
    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    @Override
    public String toString() {
        return super.toString() + "[CachingEnabled = " + isCachingEnabled() + "]";
    }
}
