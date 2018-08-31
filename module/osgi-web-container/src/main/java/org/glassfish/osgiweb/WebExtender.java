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

package org.glassfish.osgiweb;

import com.sun.enterprise.web.WebModuleDecorator;
import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * An extender that listens to web application bundle's lifecycle
 * events and does the necessary deployment/undeployment.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class WebExtender implements Extender {
    private static final Logger logger =
            Logger.getLogger(WebExtender.class.getPackage().getName());
    private BundleContext context;
    private ServiceRegistration urlHandlerService;
    private OSGiWebModuleDecorator wmd;
    private OSGiWebDeployer deployer;
    private ServiceRegistration wmdReg;

    public WebExtender(BundleContext context) {
        this.context = context;
    }

    public synchronized void start() {
        ContextPathCollisionDetector.get();
        registerWmd();
        registerDeployer();
        addURLHandler();
    }

    public synchronized void stop() {
        // Stop CollisionDetector first so that when we undeploy as part of shutting down, it won't try to deploy bundles
        ContextPathCollisionDetector.get().stop();
        removeURLHandler();
        unregisterDeployer();
        unregisterWmd();
    }

    private void registerDeployer() {
        deployer = new OSGiWebDeployer(context);
        deployer.register();
    }

    private void unregisterDeployer() {
        if (deployer != null) {
            deployer.unregister();
            deployer = null;
        }
    }

    private void addURLHandler() {
        Properties p = new Properties();
        p.put(URLConstants.URL_HANDLER_PROTOCOL,
                new String[]{Constants.WEB_BUNDLE_SCHEME});
        urlHandlerService = context.registerService(
                URLStreamHandlerService.class.getName(),
                new WebBundleURLStreamHandlerService(),
                p);
    }

    private void removeURLHandler() {
        if (urlHandlerService != null) {
            urlHandlerService.unregister();
        }
    }

    private void registerWmd() {
        wmd = new OSGiWebModuleDecorator();
        // By registering this is OSGi service registry, it will automatically make it into HK2 service registry
        // by OSGi->HK2 service mapper.
        wmdReg = this.context.registerService(WebModuleDecorator.class.getName(), wmd, null);
    }

    private void unregisterWmd() {
        if (wmdReg == null) return;
        wmdReg.unregister();
        // When we unregister the WebModuleDecorator from OSGi service registry, it also gets removed from
        // HK2 service registry. But, I am not sure if web container is able to handle dynamic services, so
        // I am making our custom decorator useless by deactivationg it which nullifies all its fields.
        wmd.deActivate();
    }

}

