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

package org.glassfish.osgijdbc;

import org.glassfish.osgijavaeebase.Extender;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.logging.Level;
import java.util.logging.Logger;


public class JDBCActivator implements BundleActivator {

    private static final Logger logger = Logger.getLogger(JDBCActivator.class.getPackage().getName());
    private ServiceRegistration extenderReg;

    public void start(BundleContext bundleContext) throws Exception {
        JDBCExtender extender = new JDBCExtender(bundleContext);
        extenderReg = bundleContext.registerService(Extender.class.getName(), extender, null);
        debug("Bundle activated");
    }

    public void stop(BundleContext bundleContext) throws Exception {
        debug("Bundle de-activated");
        extenderReg.unregister();
    }

    private void debug(String s) {
        if(logger.isLoggable(Level.FINEST)){
            logger.finest("[osgi-jdbc] : " + s);
        }
    }
}
