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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bundle provisioner.
 */
public final class BundleProvisioner {

    /**
     * List of bundles installed by a test method.
     */
    private final List<Bundle> testBundles = new ArrayList<Bundle>();

    /**
     * Bundle context.
     */
    private final BundleContext ctx;

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(
            BundleProvisioner.class.getPackage().getName());

    /**
     * Create a new instance.
     * @param bndCtx bundle context
     */
    public BundleProvisioner(final BundleContext bndCtx) {
        this.ctx = bndCtx;
    }

    /**
     * Install a bundle and add it to the list of bundles.
     *
     * @param location bundle location
     * @return Bundle
     * @throws BundleException if an error occurs
     */
    protected Bundle installTestBundle(final String location)
            throws BundleException {

        LOGGER.logp(Level.INFO, "AbstractTestObject", "installBundle",
                "Installing bundle = {0}", new Object[]{location});
        final Bundle bundle = ctx.installBundle(location);
        testBundles.add(bundle);
        LOGGER.logp(Level.INFO, "AbstractTestObject", "installBundle",
                "Installed bundle = {0} from {1} ",
                new Object[]{bundle, location});
        return bundle;
    }

    /**
     * Uninstall a bundle if it has been installed by.
     *
     * @param bundle bundle uninstall
     * @throws BundleException if an error occurs
     */
    protected void uninstallTestBundle(final Bundle bundle)
            throws BundleException {

        if (testBundles.remove(bundle)) {
            if (bundle.getState() != Bundle.UNINSTALLED) {
                LOGGER.logp(Level.INFO, "AbstractTestObject",
                        "uninstallTestBundle", "Uninstalling bundle = {0}",
                        new Object[]{bundle});
                bundle.uninstall();
                LOGGER.logp(Level.INFO, "AbstractTestObject",
                        "uninstallTestBundle", "Uninstalled bundle = {0}",
                        new Object[]{bundle});
            } else {
                LOGGER.logp(Level.INFO, "AbstractTestObject",
                        "uninstallTestBundle",
                        "Bundle Already Uninstalled = {0}",
                        new Object[]{bundle});
            }
        } else {
            throw new RuntimeException(bundle + " is not a test bundle");
        }
    }

    /**
     * Uninstall all bundles.
     * @throws BundleException if an error occurs
     */
    protected void uninstallAllTestBundles() throws BundleException {
        // take a copy because uninstallTstBundle removes from this list
        for (Bundle b : testBundles.toArray(new Bundle[0])) {
            uninstallTestBundle(b);
        }
    }
}
