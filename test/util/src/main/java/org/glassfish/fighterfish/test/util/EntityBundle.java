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
 * A class that helps tests in the deployment of entity bundles. An entity
 * bundle is a bundle containing JPA entities which upon successful deployment
 * registers a service of type EntityManagerFactory.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class EntityBundle {

    private final Bundle b;
    private final BundleContext ctx;
    private static final String[] SERVICES = {
        "javax.persistence.EntityManagerFactory"
    };

    /**
     * Create a new EntityBundle
     *
     * @param ctx BundleContext of the test - this is not the bundle context of
     * the entity bundle being deployed.
     * @param b EntityBundle being deployed.
     */
    public EntityBundle(BundleContext ctx, Bundle b) {
        this.b = b;
        this.ctx = ctx;
    }

    /**
     * Deploy this entity bundle. If a service of type EntityManagerFactory does
     * not get registered in the specified time, assume the deployment has
     * failed and throw a TimeoutException.
     *
     * @param timeout
     * @param timeUnit
     * @throws BundleException
     * @throws InterruptedException
     */
    public void deploy(long timeout, TimeUnit timeUnit)
            throws BundleException, InterruptedException {

        b.start(Bundle.START_TRANSIENT);
        for (String service : SERVICES) {
            if (OSGiUtil.waitForService(ctx, b, service,
                    timeUnit.toMillis(timeout)) == null) {
                throw new TimeoutException(
                        "Deployment timed out. No service of type "
                                + service + " found.");
            }
        }
    }

    public void undeploy() throws BundleException {
        b.stop();
    }

    public Bundle getBundle() {
        return b;
    }
}
