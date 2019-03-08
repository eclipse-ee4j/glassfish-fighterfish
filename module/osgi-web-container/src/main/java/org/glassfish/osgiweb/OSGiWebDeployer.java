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
import org.glassfish.osgijavaeebase.*;
import org.glassfish.server.ServerEnvironmentImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiWebDeployer extends AbstractOSGiDeployer {

    public OSGiWebDeployer(BundleContext context) {
        super(context, Integer.MAX_VALUE);
    }

    @Override
    public OSGiUndeploymentRequest createOSGiUndeploymentRequest(
            Deployment deployer, ServerEnvironmentImpl env,
            ActionReport reporter, OSGiApplicationInfo osgiAppInfo) {
        return new OSGiWebUndeploymentRequest(deployer, env, reporter,
                osgiAppInfo);
    }

    @Override
    public OSGiDeploymentRequest createOSGiDeploymentRequest(
            Deployment deployer, ArchiveFactory archiveFactory,
            ServerEnvironmentImpl env, ActionReport reporter, Bundle b) {
        return new OSGiWebDeploymentRequest(deployer, archiveFactory, env,
                reporter, b);
    }

    @Override
    public boolean handles(Bundle bundle) {
        return isWebBundle(bundle);
    }

    /**
     * Determines if a bundle represents a web application or not. As per rfc
     * #66, a web container extender recognizes a web application bundle by
     * looking for the presence of Web-contextPath manifest header
     *
     * @param b
     * @return
     */
    private boolean isWebBundle(Bundle b) {
        final Dictionary headers = b.getHeaders();
        return headers.get(Constants.WEB_CONTEXT_PATH) != null
                && headers.get(org.osgi.framework.Constants.FRAGMENT_HOST)
                == null;
    }

    @Override
    protected void raiseEvent(State state, Bundle appBundle, Throwable e) {
        WABEventPublisher ep = new WABEventPublisher();
        ep.raiseEvent(state, appBundle, getBundleContext().getBundle(), e);
    }

}
