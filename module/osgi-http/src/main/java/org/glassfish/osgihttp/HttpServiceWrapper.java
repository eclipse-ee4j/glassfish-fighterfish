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
package org.glassfish.osgihttp;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

/**
 * This is an implementation of {@link HttpService} per bundle. This is what a
 * bundle gets when they look up the service in OSGi service registry. This is
 * needed so that we can unregister all the servlets registered by a bundle when
 * that bundle goes down without unregistering the servlet or resource end
 * points. This delegates to {@link GlassFishHttpService} for implementing the
 * actual service.
 */
public final class HttpServiceWrapper implements HttpService {

    /**
     * The delegate HTTP service.
     */
    private final GlassFishHttpService delegate;

    /**
     * The bundle which has looked up this service instance from registry.
     */
    private final Bundle registeringBundle;

    /**
     * Aliases registered by the current bundle holding this service reference.
     */
    private final Set<String> aliases = new HashSet<String>();

    /**
     * Create a new instance.
     * @param gfHttpService the delegate HTTP service
     * @param bnd the registering bundle
     */
    public HttpServiceWrapper(final GlassFishHttpService gfHttpService,
            final Bundle bnd) {

        this.delegate = gfHttpService;
        this.registeringBundle = bnd;
    }

    @Override
    public HttpContext createDefaultHttpContext() {
        return new DefaultHttpContext(registeringBundle);
    }

    @Override
    public void registerServlet(final String alias, final Servlet servlet,
            final Dictionary initParams, final HttpContext httpContext)
            throws ServletException, NamespaceException {

        HttpContext ctx;
        if (httpContext != null) {
            ctx = httpContext;
        } else {
            ctx = createDefaultHttpContext();
        }
        delegate.registerServlet(alias, servlet, initParams, ctx);
        aliases.add(alias);
    }

    @Override
    public void registerResources(final String alias, final String name,
            final HttpContext httpContext) throws NamespaceException {

        HttpContext ctx;
        if (httpContext != null) {
            ctx = httpContext;
        } else {
            ctx = createDefaultHttpContext();
        }
        delegate.registerResources(alias, name, ctx);
        aliases.add(alias);
    }

    @Override
    public synchronized void unregister(final String alias) {
        unregister(alias, true);
    }

    /**
     * Unregister a given alias.
     * @param alias the alias to unregister
     * @param callDestroy flag to indicate if servlet.destroy should be called
     */
    private void unregister(final String alias, final boolean callDestroy) {
        delegate.unregister(alias, callDestroy);
        aliases.remove(alias);
    }

    /**
     * Unregisters all the aliases without calling servlet.destroy (if any).
     */
    void unregisterAll() {
        // take a copy of all registered aliases,
        // as the underlying list will change
        for (String alias : aliases.toArray(new String[0])) {
            unregister(alias, false); // don't call servlet.destry, hence false
        }
    }

    /**
     * This service factory is needed, because the spec requires the following:
     * If the bundle which performed the registration is stopped or otherwise
     * "unget"s the Http Service without calling unregister(java.lang.String)
     * then Http Service must automatically unregister the registration.
     * However, if the registration was for a servlet, the destroy method of the
     * servlet will not be called in this case since the bundle may be stopped.
     * unregister(java.lang.String) must be explicitly called to cause the
     * destroy method of the servlet to be called. This can be done in the
     * BundleActivator.stop method of the bundle registering the servlet.
     */
    public static final class HttpServiceFactory implements ServiceFactory {

        /**
         * Delegate HTTP service.
         */
        private final GlassFishHttpService delegate;

        /**
         * Create a new instance.
         * @param gfHttpService the delegate HTTP service
         */
        public HttpServiceFactory(final GlassFishHttpService gfHttpService) {
            this.delegate = gfHttpService;
        }

        @Override
        public Object getService(final Bundle bnd,
                final ServiceRegistration registration) {

            return new HttpServiceWrapper(delegate, bnd);
        }

        @Override
        public void ungetService(final Bundle bundle,
                final ServiceRegistration registration, final Object service) {

            HttpServiceWrapper.class.cast(service).unregisterAll();
        }
    }
}
