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

import com.sun.enterprise.web.WebModuleDecorator;
import java.util.Dictionary;
import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

import java.util.Properties;

/**
 * An extender that listens to web application bundle's lifecycle
 * events and does the necessary deployment/undeployment.
 */
public final class WebExtender implements Extender {

    /**
     * The bundle context.
     */
    private final BundleContext context;

    /**
     * Service registration for the URL handler service.
     */
    private ServiceRegistration urlHandlerService;

    /**
     * OSGi web module decorator.
     */
    private OSGiWebModuleDecorator wmd;

    /**
     * Deployer.
     */
    private OSGiWebDeployer deployer;

    /**
     * Service registration for the web module decorator.
     */
    private ServiceRegistration wmdReg;

    /**
     * Create a new instance.
     * @param bndCtx the bundle context
     */
    public WebExtender(final BundleContext bndCtx) {
        this.context = bndCtx;
    }

    @Override
    public synchronized void start() {
        ContextPathCollisionDetector.get();
        registerWmd();
        registerDeployer();
        addURLHandler();
    }

    @Override
    public synchronized void stop() {
        // Stop CollisionDetector first so that when we undeploy as part of
        // shutting down, it won't try to deploy bundles
        ContextPathCollisionDetector.get().stop();
        removeURLHandler();
        unregisterDeployer();
        unregisterWmd();
    }

    /**
     * Register the deployer.
     */
    private void registerDeployer() {
        deployer = new OSGiWebDeployer(context);
        deployer.register();
    }

    /**
     * Unregister the deployer.
     */
    private void unregisterDeployer() {
        if (deployer != null) {
            deployer.unregister();
            deployer = null;
        }
    }

    /**
     * Register the URL handler service.
     */
    @SuppressWarnings("unchecked")
    private void addURLHandler() {
        Dictionary props = new Properties();
        props.put(URLConstants.URL_HANDLER_PROTOCOL,
                new String[]{Constants.WEB_BUNDLE_SCHEME});
        urlHandlerService = context.registerService(
                URLStreamHandlerService.class.getName(),
                new WebBundleURLStreamHandlerService(),
                props);
    }

    /**
     * Unregister the URL handler.
     */
    private void removeURLHandler() {
        if (urlHandlerService != null) {
            urlHandlerService.unregister();
        }
    }

    /**
     * Register the web module decorator as a service.
     */
    private void registerWmd() {
        wmd = new OSGiWebModuleDecorator();
        // By registering this is OSGi service registry, it will automatically
        // make it into HK2 service registry
        // by OSGi->HK2 service mapper.
        wmdReg = this.context.registerService(
                WebModuleDecorator.class.getName(), wmd, null);
    }

    /**
     * Unregister the web module decorator.
     */
    private void unregisterWmd() {
        if (wmdReg == null) {
            return;
        }
        wmdReg.unregister();
        // When we unregister the WebModuleDecorator from OSGi service registry,
        // it also gets removed from
        // HK2 service registry. But, I am not sure if web container is able to
        // handle dynamic services, so
        // I am making our custom decorator useless by deactivationg it which
        // nullifies all its fields.
        wmd.deActivate();
    }
}
