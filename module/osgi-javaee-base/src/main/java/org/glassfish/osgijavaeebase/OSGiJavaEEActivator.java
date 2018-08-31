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

package org.glassfish.osgijavaeebase;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import static org.glassfish.osgijavaeebase.OSGiBundleArchive.EmbeddedJarURLStreamHandlerService;
import static org.glassfish.osgijavaeebase.OSGiBundleArchive.EmbeddedJarURLStreamHandlerService.*;

import java.util.Properties;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiJavaEEActivator implements BundleActivator {

    private ExtenderManager extenderManager;
    private ServiceRegistration urlHandlerServiceRegistration;
    private ServiceRegistration javaeeExtenderServiceRegistration;

    public void start(BundleContext context) throws Exception {
        addURLHandler(context);
        extenderManager = new ExtenderManager(context);
        extenderManager.start();
        addExtender(context);
    }

    public void stop(BundleContext context) throws Exception {
        removeExtender();
        extenderManager.stop();
        removeURLHandler(context);
    }

    private void addExtender(BundleContext context) {
        JavaEEExtender extender = new JavaEEExtender(context);
        javaeeExtenderServiceRegistration = context.registerService(Extender.class.getName(), extender, null);
    }

    private void removeExtender() {
        javaeeExtenderServiceRegistration.unregister();
    }

    private void addURLHandler(BundleContext context) {
        Properties p = new Properties();
        p.setProperty(URLConstants.URL_HANDLER_PROTOCOL, EMBEDDED_JAR_SCHEME);
        urlHandlerServiceRegistration = context.registerService(URLStreamHandlerService.class.getName(), new EmbeddedJarURLStreamHandlerService(), p);
    }

    private void removeURLHandler(BundleContext context) {
        if (urlHandlerServiceRegistration != null) urlHandlerServiceRegistration.unregister();
    }
}
