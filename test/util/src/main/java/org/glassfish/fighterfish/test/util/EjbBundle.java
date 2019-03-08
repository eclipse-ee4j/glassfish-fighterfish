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
 * if b.start() returns successfully or not. This is where this class is helpful.
 * Unlike OSGi Web Application container, OSGi EJB Spec does not raise events to
 * indicating success or failure of events. So, this class relies on user to
 * tell it at least one service that's being exported by this bundle to OSGi
 * service registry. This class then uses a service tracker to wait for such a
 * service to appear. If such a service does not show up in a specified amount
 * of time, it times out the deployment operation.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class EjbBundle {

    private final BundleContext ctx;
    private final Bundle b;
    private final String[] services;

    /**
     * A handle to the EJB bundle being deployed.
     *
     * @param ctx BundleContext of test used for various OSGi operation.
     * @param b Bundle to be deployed.
     * @param services Services that are expected to be made available by this
     * EJB bundle if deployment is successful.
     */
    public EjbBundle(BundleContext ctx, Bundle b, String[] services) {
        this.ctx = ctx;
        this.b = b;
        this.services = services;
    }

    /**
     * Deploy the given EJB OSGi bundle. Deployment is triggered asynchronously
     * by starting the bundle. If none of the user specified services show up in
     * service registry in the specified amount of time, it assumes the
     * operation has failed and throws TimeoutOperation.
     *
     * @param timeout
     * @param timeUnit
     * @throws BundleException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void deploy(long timeout, TimeUnit timeUnit)
            throws BundleException, InterruptedException, TimeoutException {

        b.start(Bundle.START_TRANSIENT);
        for (String service : services) {
            if (OSGiUtil.waitForService(ctx, b, service,
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
     * @throws BundleException
     */
    public void undeploy() throws BundleException {
        b.stop();
    }

    public Bundle getBundle() {
        return b;
    }
}
