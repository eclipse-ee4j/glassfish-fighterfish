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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * OSGi utility class.
 */
public final class OSGiUtil {

    /**
     * Cannot be instanciated.
     */
    private OSGiUtil() {
    }

    /**
     * Get a service by type.
     * @param <T> service type parameter
     * @param ctx bundle context
     * @param type service class
     * @return T
     */
    // TODO(Sahoo): Move the functionality to TestContext so that the service
    // references can be closed upon end of test
    public static <T> T getService(BundleContext ctx, Class<T> type) {

        ServiceTracker st = new ServiceTracker(ctx, type.getName(), null);
        st.open();
        try {
            return type.cast(st.getService());
        } finally {
            // st.close();
        }
    }

    /**
     * Get a service by type.
     * @param <T> service type parameter
     * @param ctx bundle context
     * @param type service type
     * @param timeout wait timeout
     * @return T
     * @throws InterruptedException if an error occurs while waiting
     */
    public static <T> T getService(BundleContext ctx, Class<T> type, long timeout) throws InterruptedException {
        ServiceTracker<T, T> serviceTracker = new ServiceTracker<>(ctx, type.getName(), null);
        serviceTracker.open();
        try {
            return type.cast(serviceTracker.waitForService(timeout));
        } finally {
            // st.close();
        }
    }

    /**
     * Wait for a specified amount of time for a service of a given type to be
     * made available by a given bundle.
     *
     * @param ctx BundleContext that should be used to track the service
     * @param bnd Bundle registering the service
     * @param serviceClass FQN of the service type
     * @param timeout no of milliseconds to wait for the service to be available
     * before returning null
     * @return a reference to the service being tracked
     * @throws InterruptedException if an error occurs while waiting
     */
    public static Object waitForService(BundleContext ctx, Bundle bnd, String serviceClass, long timeout) throws InterruptedException {

        ServiceTracker<?, ?> serviceTracker = new ServiceTracker<Object, Object>(ctx, serviceClass, null) {
            @Override
            public Object addingService(ServiceReference<Object> reference) {
                if (reference.getBundle() == bnd) {
                    return reference;
                } 
                
                return null;
            }

            @Override
            public void removedService(ServiceReference<Object> reference, Object service) {

                // no need to unget, as we don't get the service
                // in addingService
            }
        };
        serviceTracker.open(false);
        Object serviceInstance;
        try {
            serviceInstance = serviceTracker.waitForService(timeout);
        } finally {
            // st.close();
        }
        
        return serviceInstance;
    }
}
