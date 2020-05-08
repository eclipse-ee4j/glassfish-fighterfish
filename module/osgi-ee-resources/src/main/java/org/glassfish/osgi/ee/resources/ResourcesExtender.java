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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.BundleContext;

/**
 * EE resources extender.
 */
public final class ResourcesExtender implements Extender {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ResourcesExtender.class.getPackage().getName());

    /**
     * Bundle context.
     */
    private final BundleContext bundleContext;

    /**
     * Component locator.
     */
    private Habitat habitat;

    /**
     * Resource provider service.
     */
    private ResourceProviderService rps;

    /**
     * Create a new instance.
     *
     * @param context bundle context
     */
    public ResourcesExtender(final BundleContext context) {
        this.bundleContext = context;
    }

    @Override
    public void start() {
        debug("begin start()");
        GlassFish gf = (GlassFish) bundleContext.getService(bundleContext.getServiceReference(GlassFish.class.getName()));
        habitat = new Habitat(gf);
        rps = new ResourceProviderService(habitat, bundleContext);
        rps.registerResources();
        debug("completed start()");
    }

    @Override
    public void stop() {
        rps.unRegisterResources();
        debug("stopped");
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
