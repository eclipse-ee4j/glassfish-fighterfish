/*
 * Copyright (c) 2012, 2019 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.fighterfish.test.app20;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * A{@code BundleActivator} that registers the simple stock quote
 * service implementation in the OSGi Service registry.
 */
public final class SimpleServiceActivator implements BundleActivator {

    @Override
    public void start(final BundleContext context) throws Exception {
        System.out.println("SimpleServiceActivator::start");
        context.registerService(StockQuoteService.class.getName(),
                new SimpleStockQuoteServiceImpl(),
                null);
        System.out.println("SimpleServiceActivator::registration of Stock"
                + " quote service successful");
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        System.out.println("SimpleServiceActivator stopped");
    }
}
