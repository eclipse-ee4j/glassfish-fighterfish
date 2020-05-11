/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.fighterfish.test.util;

import static org.glassfish.embeddable.GlassFish.Status.STARTED;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * When GlassFish bundles are deployed, the server does a lot of background
 * operation, so this class help test cases track availability of GlassFish. It
 * provides a convenient mechanism for tests to wait for GlassFish server to
 * start.
 */
public final class GlassFishTracker {
  
    /**
     * Wait for GlassFish to start.
     * @param context bundle context
     * @param timeout wait timeout
     * @return GlassFish
     * @throws InterruptedException if an error occurs
     * @throws GlassFishException if an error occurs
     */
    public static GlassFish waitForGfToStart(BundleContext context, long timeout) throws InterruptedException, GlassFishException {

        ServiceTracker<?, GlassFish> serviceTracker = new ServiceTracker<>(context,GlassFish.class.getName(), null);
        serviceTracker.open();
        GlassFish glassFish;
        
        long currentTime = System.currentTimeMillis();
        try {
            glassFish = serviceTracker.waitForService(timeout);
        } finally {
            serviceTracker.close();
        }
        
        if (glassFish == null) {
            throw new TimeoutException("GlassFish service is still not available after " + timeout + " ms.");
        }
        
        long endTime = currentTime + timeout;
        while (glassFish.getStatus() != STARTED && System.currentTimeMillis() < endTime) {
            Thread.sleep(100);
        }
        
        if (glassFish.getStatus() != STARTED) {
            throw new TimeoutException("GlassFish has not started after " + timeout + " ms.");
        }
        
        return glassFish;
    }
}
