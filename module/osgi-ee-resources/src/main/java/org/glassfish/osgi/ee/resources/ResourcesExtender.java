/*
 * Copyright (c) 2010, 2018 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.embeddable.GlassFish;
import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.*;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ResourcesExtender implements Extender {

    private static final Logger LOGGER = Logger.getLogger(
            ResourcesExtender.class.getPackage().getName());

    private final BundleContext bundleContext;
    private Habitat habitat;
    private ResourceProviderService rps;


    public ResourcesExtender(BundleContext context) {
        this.bundleContext = context;
    }

    @Override
    public void start() {
        debug("begin start()");
        GlassFish gf = (GlassFish) bundleContext
                .getService(bundleContext.getServiceReference(
                        GlassFish.class.getName()));
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

    private void debug(String s) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "[osgi-ee-resources] : {0}", s);
        }
    }
}
