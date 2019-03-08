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
import static org.glassfish.osgijavaeebase.OSGiBundleArchive.EmbeddedJarURLStreamHandlerService.*;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiJavaEEActivator implements BundleActivator {

    private ExtenderManager extenderManager;
    private ServiceRegistration urlHandlerServiceRegistration;
    private ServiceRegistration javaeeExtenderServiceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        addURLHandler(context);
        extenderManager = new ExtenderManager(context);
        extenderManager.start();
        addExtender(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        removeExtender();
        extenderManager.stop();
        removeURLHandler();
    }

    private void addExtender(BundleContext context) {
        JavaEEExtender extender = new JavaEEExtender(context);
        javaeeExtenderServiceRegistration = context
                .registerService(Extender.class.getName(), extender, null);
    }

    private void removeExtender() {
        javaeeExtenderServiceRegistration.unregister();
    }

    @SuppressWarnings("unchecked")
    private void addURLHandler(BundleContext context) {
        Dictionary p = new Properties();
        p.put(URLConstants.URL_HANDLER_PROTOCOL, EMBEDDED_JAR_SCHEME);
        urlHandlerServiceRegistration = context
                .registerService(URLStreamHandlerService.class.getName(),
                        new EmbeddedJarURLStreamHandlerService(), p);
    }

    private void removeURLHandler() {
        if (urlHandlerServiceRegistration != null) {
            urlHandlerServiceRegistration.unregister();
        }
    }
}
