/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.uas.simpleservice;

import org.glassfish.fighterfish.sample.uas.api.UserAuthService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Bundle activator for this service.
 */
public final class SimpleActivator implements BundleActivator {

    @Override
    public void start(final BundleContext context) throws Exception {
        context.registerService(UserAuthService.class.getName(), new UserAuthServiceImpl(), null);
        log("Registered service");
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        // No need to unregister, as the service gets unregistered
        // automatically when bundle stops.
    }

    /**
     * Log a message.
     * 
     * @param msg message to log
     */
    static void log(final String msg) {
        System.out.println("UserAuthService: " + msg);
    }

}
