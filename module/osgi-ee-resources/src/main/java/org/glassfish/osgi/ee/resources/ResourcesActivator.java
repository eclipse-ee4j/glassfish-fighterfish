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
package org.glassfish.osgi.ee.resources;

import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bundle activator for the OSGi EE module.
 */
public final class ResourcesActivator implements BundleActivator {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ResourcesActivator.class.getPackage().getName());

    /**
     * Service registration for the extender service.
     */
    private ServiceRegistration extenderReg;

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        ResourcesExtender extender = new ResourcesExtender(bundleContext);
        extenderReg = bundleContext.registerService(Extender.class.getName(), extender, null);
        debug("Bundle activated");
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
        debug("Bundle de-activated");
        extenderReg.unregister();
    }

    /**
     * Log a message at the {@code FINEST} level.
     * 
     * @param msg message to log
     */
    private void debug(final String msg) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "[osgi-ee-resources] : {0}", msg);
        }
    }
}
