/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.fighterfish.test.ct.misc.bundle.delay;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * A simple bundle that just introduces a delay in the system. This is needed
 * because some of the CT tests expect services to be immediately available.
 */
public final class DelayActivator implements BundleActivator {

    @Override
    public void start(final BundleContext ctx) throws Exception {
        String v = ctx.getProperty("fighterfish.test.ct.misc.bundle.delay");
        if (v == null) {
            v = "10000";
        }
        System.out.println(
                "fighterfish.test.ct.misc.bundle.delay.DelayActivator"
                + " will delay proceedings by " + v + " ms.");
        Thread.sleep(Long.parseLong(v));
    }

    @Override
    public void stop(final BundleContext ctx) {
    }
}
