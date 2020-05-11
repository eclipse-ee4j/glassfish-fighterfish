/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.osgiejb;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.osgijavaeebase.OSGiDeploymentContext;
import org.glassfish.osgijavaeebase.OSGiDeploymentRequest;
import org.glassfish.server.ServerEnvironmentImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import com.sun.enterprise.deploy.shared.ArchiveFactory;

/**
 * Custom deployment request for the OSGi EJB container.
 */
public final class OSGiEJBDeploymentRequest extends OSGiDeploymentRequest {

    /**
     * Create a new instance.
     *
     * @param deployer GlassFish deployer
     * @param archiveFactory GlassFish archive factory
     * @param env GlassFish server environment
     * @param reporter GlassFish command reporter
     * @param bnd application bundle
     */
    public OSGiEJBDeploymentRequest(Deployment deployer, ArchiveFactory archiveFactory, ServerEnvironmentImpl env, ActionReport reporter, Bundle bnd) {
        super(deployer, archiveFactory, env, reporter, bnd);
    }

    @Override
    protected OSGiDeploymentContext getDeploymentContextImpl(ActionReport reporter, Logger logger, ReadableArchive archive, OpsParams opsParams, ServerEnvironmentImpl env, final Bundle bnd) throws Exception {
        return new OSGiEJBDeploymentContext(reporter, logger, archive, opsParams, env, bnd);
    }

    @Override
    protected EJBBundle makeArchive() {
        Bundle host = getBundle();
        
        return new EJBBundle(getFragments(host), host);
    }

    /**
     * Get the bundle fragments of a given host bundle.
     *
     * @param host the host bundle
     * @return Bundle[]
     */
    private static Bundle[] getFragments(final Bundle host) {
        List<Bundle> fragments = new ArrayList<>();
        BundleWiring hostWiring = host.adapt(BundleWiring.class);
        for (BundleWire wire : hostWiring.getProvidedWires(HostNamespace.HOST_NAMESPACE)) {
            fragments.add(wire.getRequirerWiring().getBundle());
        }
        
        return fragments.toArray(new Bundle[fragments.size()]);
    }
}
