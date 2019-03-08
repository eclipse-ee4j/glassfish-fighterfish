/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.glassfish.fighterfish.sample.osgihttp.helloworld;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This activator demonstrates how to use HttpService using a ServiceTracker. It
 * demonstrates registerReosurce method of HttpService.
 *
 * The secondary reason for having this activator is that it causes HttpService
 * class to be loaded which in turn activates osgi-http bundle of GlassFish,
 * which uses lazy activation policy.
 */
public final class HelloWorldHttpActivator implements BundleActivator {

    /**
     * Service tracker for the OSGi HTTP service.
     */
    private volatile ServiceTracker tracker;

    @Override
    @SuppressWarnings("unchecked")
    public void start(final BundleContext context) throws Exception {
        tracker = new ServiceTracker(context, HttpService.class.getName(),
                null) {

            @Override
            public Object addingService(final ServiceReference reference) {
                HttpService http = HttpService.class.cast(
                        context.getService(reference));
                try {
                    // maps hello.html to helloworld.html
                    http.registerResources("/hello.html", "helloworld.html",
                            null);
                } catch (NamespaceException e) {
                    e.printStackTrace();
                }
                return super.addingService(reference);
            }

            @Override
            public void removedService(final ServiceReference reference,
                    final Object service) {

                HttpService http = HttpService.class.cast(
                        context.getService(reference));
                try {
                    http.unregister("/hello.html");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.removedService(reference, service);
            }
        };
        tracker.open();
        System.out.println("HelloWorldHttpActivator.start()");
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        tracker.close();
        tracker = null;
    }
}
