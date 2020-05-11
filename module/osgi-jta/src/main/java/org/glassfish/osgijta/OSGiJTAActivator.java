/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.osgijta;

import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Bundle activator for the OSGi JTA module.
 */
public final class OSGiJTAActivator implements BundleActivator {

    /**
     * Service registration for the JTA extender service.
     */
    private ServiceRegistration<?> extenderReg;

    @Override
    public void start(BundleContext context) throws Exception {
        extenderReg = context.registerService(Extender.class.getName(), new JTAExtender(context), null);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        extenderReg.unregister();
    }
}
