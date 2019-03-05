/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
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
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiUtil {

    // TODO(Sahoo): Move the functionality to TestContext so that the service
    // references can be closed upon end of test
    @SuppressWarnings("unchecked")
    public static <T> T getService(BundleContext ctx, Class<T> type) {
        ServiceTracker st = new ServiceTracker(ctx, type.getName(), null);
        st.open();
        try {
            return type.cast(st.getService());
        } finally {
//            st.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getService(BundleContext ctx, Class<T> type,
            long timeout)
            throws InterruptedException {

        ServiceTracker<T,T> st = new ServiceTracker(ctx, type.getName(), null);
        st.open();
        try {
            return type.cast(st.waitForService(timeout));
        } finally {
//            st.close();
        }
    }

    /**
     * Wait for a specified amount of time for a service of a given type to be
     * made available by a given bundle.
     *
     * @param ctx BundleContext that should be used to track the service
     * @param b Bundle registering the service
     * @param service FQN of the service type
     * @param timeout no of milliseconds to wait for the service to be available
     * before returning null
     * @return a reference to the service being tracked
     * @throws InterruptedException
     */
    @SuppressWarnings("unchecked")
    public static Object waitForService(BundleContext ctx, final Bundle b,
            String service, long timeout)
            throws InterruptedException {

        ServiceTracker st = new ServiceTracker(ctx, service, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                if (reference.getBundle() == b) {
                    return reference;
                } else {
                    return null;
                }
            }

            @Override
            public void removedService(ServiceReference reference,
                    Object service) {
                // no need to unget, as we don't get the service
                // in addingService
            }
        };
        st.open(false);
        Object s;
        try {
            s = st.waitForService(timeout);
        } finally {
//            st.close();
        }
        return s;
    }
}
