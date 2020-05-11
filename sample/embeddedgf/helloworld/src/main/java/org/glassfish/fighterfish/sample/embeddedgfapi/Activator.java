/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.embeddedgfapi;

import org.glassfish.embeddable.GlassFish;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Bundle activator.
 */
public final class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        ServiceTracker<?, GlassFish> serviceTracker = new ServiceTracker<>(context, GlassFish.class.getName(), null);
        GlassFish glassFish = serviceTracker.waitForService(0);
        
        System.out.println(glassFish.getStatus());
        serviceTracker.close();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
