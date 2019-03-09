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
package org.glassfish.osgiweb;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.ActionReport;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.server.ServerEnvironmentImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import org.glassfish.osgijavaeebase.AbstractOSGiDeployer;
import org.glassfish.osgijavaeebase.AbstractOSGiDeployer.State;
import org.glassfish.osgijavaeebase.OSGiApplicationInfo;
import org.glassfish.osgijavaeebase.OSGiDeploymentRequest;
import org.glassfish.osgijavaeebase.OSGiUndeploymentRequest;

/**
 * Deployer implementation for OSGi web applications.
 */
public final class OSGiWebDeployer extends AbstractOSGiDeployer {

    /**
     * Create a new instance.
     * @param context the bundle context
     */
    public OSGiWebDeployer(final BundleContext context) {
        super(context, Integer.MAX_VALUE);
    }

    @Override
    public OSGiUndeploymentRequest createOSGiUndeploymentRequest(
            final Deployment deployer, final ServerEnvironmentImpl env,
            final ActionReport reporter,
            final OSGiApplicationInfo osgiAppInfo) {

        return new OSGiWebUndeploymentRequest(deployer, env, reporter,
                osgiAppInfo);
    }

    @Override
    public OSGiDeploymentRequest createOSGiDeploymentRequest(
            final Deployment deployer, final ArchiveFactory archiveFactory,
            final ServerEnvironmentImpl env, final ActionReport reporter,
            final Bundle bnd) {

        return new OSGiWebDeploymentRequest(deployer, archiveFactory, env,
                reporter, bnd);
    }

    @Override
    public boolean handles(final Bundle bundle) {
        return isWebBundle(bundle);
    }

    /**
     * Determines if a bundle represents a web application or not. As per rfc
     * #66, a web container extender recognizes a web application bundle by
     * looking for the presence of Web-contextPath manifest header
     *
     * @param bundle application bundle
     * @return {@code true} if the bundle is a web bundle, {@code false}
     * otherwise
     */
    private boolean isWebBundle(final Bundle bundle) {
        final Dictionary headers = bundle.getHeaders();
        return headers.get(Constants.WEB_CONTEXT_PATH) != null
                && headers.get(org.osgi.framework.Constants.FRAGMENT_HOST)
                == null;
    }

    @Override
    protected void raiseEvent(final State state, final Bundle appBundle,
            final Throwable e) {

        WABEventPublisher ep = new WABEventPublisher();
        ep.raiseEvent(state, appBundle, getBundleContext().getBundle(), e);
    }
}
