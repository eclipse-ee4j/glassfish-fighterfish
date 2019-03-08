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
package org.glassfish.osgijavaeebase;

import java.util.Dictionary;
import java.util.Properties;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.service.url.URLConstants;

import static org.glassfish.osgijavaeebase.OSGiBundleArchive.EmbeddedJarURLStreamHandlerService;
import static org.glassfish.osgijavaeebase.OSGiBundleArchive.EmbeddedJarURLStreamHandlerService.EMBEDDED_JAR_SCHEME;

/**
 * Bundle activator that registers {@link JavaEEExtender} as a service.
 */
public final class OSGiJavaEEActivator implements BundleActivator {

    /**
     * The extender manager.
     */
    private ExtenderManager extenderManager;

    /**
     * The service registration for {@link EmbeddedJarURLStreamHandlerService}.
     */
    private ServiceRegistration urlHandlerServiceRegistration;

    /**
     * The service registration for {@link JavaEEExtender}.
     */
    private ServiceRegistration javaeeExtenderServiceRegistration;

    @Override
    public void start(final BundleContext context) throws Exception {
        addURLHandler(context);
        extenderManager = new ExtenderManager(context);
        extenderManager.start();
        addExtender(context);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        removeExtender();
        extenderManager.stop();
        removeURLHandler();
    }

    /**
     * Create an instance of {@link JavaEEExtender} and register it as an OSGi
     * service.
     * @param context the bundle context
     */
    private void addExtender(final BundleContext context) {
        JavaEEExtender extender = new JavaEEExtender(context);
        javaeeExtenderServiceRegistration = context
                .registerService(Extender.class.getName(), extender, null);
    }

    /**
     * Unregisters the extender service.
     */
    private void removeExtender() {
        javaeeExtenderServiceRegistration.unregister();
    }

    /**
     * Create a new instance of {@link EmbeddedJarURLStreamHandlerService} and
     * register it as an OSGi service.
     * @param context the bundle context
     */
    @SuppressWarnings("unchecked")
    private void addURLHandler(final BundleContext context) {
        Dictionary p = new Properties();
        p.put(URLConstants.URL_HANDLER_PROTOCOL, EMBEDDED_JAR_SCHEME);
        urlHandlerServiceRegistration = context
                .registerService(URLStreamHandlerService.class.getName(),
                        new EmbeddedJarURLStreamHandlerService(), p);
    }

    /**
     * Unregisters the URL handler service.
     */
    private void removeURLHandler() {
        if (urlHandlerServiceRegistration != null) {
            urlHandlerServiceRegistration.unregister();
        }
    }
}
