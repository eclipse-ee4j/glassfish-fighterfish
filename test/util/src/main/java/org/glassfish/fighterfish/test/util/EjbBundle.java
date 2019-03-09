/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
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
import org.osgi.framework.BundleException;

import java.util.concurrent.TimeUnit;

/**
 * This class is used by tests to deploy Ejb Bundles. Since EJB deployment
 * happens asynchronously when an EJB Bundle is activated, for a test case to
 * know whether the deployment is successful or not is not as simple as checking
 * if b.start() returns successfully or not. This is where this class is
 * helpful. Unlike OSGi Web Application container, OSGi EJB Spec does not raise
 * events to indicating success or failure of events. So, this class relies on
 * user to tell it at least one service that's being exported by this bundle to
 * OSGi service registry. This class then uses a service tracker to wait for
 * such a service to appear. If such a service does not show up in a specified
 * amount of time, it times out the deployment operation.
 */
public final class EjbBundle {

    /**
     * Bundle context.
     */
    private final BundleContext ctx;

    /**
     * Bundle.
     */
    private final Bundle bundle;

    /**
     * Services provided by the bundle.
     */
    private final String[] services;

    /**
     * Create a new instance.
     *
     * @param bndCtx BundleContext of test used for various OSGi operation.
     * @param bnd Bundle to be deployed.
     * @param svcs Services that are expected to be made available by this
     * EJB bundle if deployment is successful.
     */
    public EjbBundle(final BundleContext bndCtx, final Bundle bnd,
            final String[] svcs) {

        this.ctx = bndCtx;
        this.bundle = bnd;
        this.services = svcs;
    }

    /**
     * Deploy the given EJB OSGi bundle. Deployment is triggered asynchronously
     * by starting the bundle. If none of the user specified services show up in
     * service registry in the specified amount of time, it assumes the
     * operation has failed and throws TimeoutOperation.
     *
     * @param timeout deploy timeout
     * @param timeUnit timeout unit
     * @throws BundleException if an error occurs
     * @throws InterruptedException if an error occurs
     * @throws TimeoutException if the timeout is reached
     */
    public void deploy(final long timeout, final TimeUnit timeUnit)
            throws BundleException, InterruptedException, TimeoutException {

        bundle.start(Bundle.START_TRANSIENT);
        for (String service : services) {
            if (OSGiUtil.waitForService(ctx, bundle, service,
                    timeUnit.toMillis(timeout)) == null) {
                throw new TimeoutException(
                        "Deployment timed out. No service of type "
                                + service + " found.");
            }
        }
    }

    /**
     * Undeploy the EJB OSGi bundle. There is no need for any timeout argument,
     * as undeployment is a synchronous process unlike deployment.
     *
     * @throws BundleException if an error occurs
     */
    public void undeploy() throws BundleException {
        bundle.stop();
    }

    /**
     * Get the bundle.
     * @return Bundle
     */
    public Bundle getBundle() {
        return bundle;
    }
}
