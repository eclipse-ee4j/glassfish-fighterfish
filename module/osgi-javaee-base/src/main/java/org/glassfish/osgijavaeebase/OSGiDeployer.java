/*
 * Copyright (c) 2009, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgijavaeebase;

import org.osgi.framework.Bundle;

/**
 * Base interface for deploying OSGi applications.
 */
public interface OSGiDeployer {

    /**
     * Deploy the given bundle.
     * @param bdn the bundle to deploy
     * @return OSGiApplicationInfo
     * @throws DeploymentException if an error occurs
     */
    OSGiApplicationInfo deploy(Bundle bdn) throws DeploymentException;

    /**
     * Undeploy the given bundle.
     * @param osgiAppInfo the deployed application info
     * @throws DeploymentException if an error occurs
     */
    void undeploy(OSGiApplicationInfo osgiAppInfo) throws DeploymentException;

    /**
     * Test if this deployer handles the given bundle.
     * @param bundle the bundle to test
     * @return {@code true} if this deployer handles the given bundle,
     * {@code false} otherwise
     */
    boolean handles(Bundle bundle);
}
