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
package org.glassfish.osgiejb;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiEJBContainerActivator implements BundleActivator {

    private ServiceRegistration ejbExtenderReg;

    @Override
    public void start(BundleContext context) throws Exception {
        EJBExtender ejbExtender = new EJBExtender(context);
        ejbExtenderReg = context.registerService(Extender.class.getName(),
                ejbExtender, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Unregister so that ExtenderManager can stop Extender with an active
        // bundle ctx
        ejbExtenderReg.unregister();
    }
}
