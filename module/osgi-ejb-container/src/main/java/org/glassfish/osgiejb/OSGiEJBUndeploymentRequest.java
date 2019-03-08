/*
 * Copyright (c) 2010, 2019 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.osgijavaeebase.OSGiUndeploymentRequest;
import org.glassfish.osgijavaeebase.OSGiApplicationInfo;
import org.glassfish.osgijavaeebase.OSGiDeploymentContext;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.server.ServerEnvironmentImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.osgi.framework.Bundle;

import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiEJBUndeploymentRequest extends OSGiUndeploymentRequest {

    public OSGiEJBUndeploymentRequest(Deployment deployer, ServerEnvironmentImpl env, ActionReport reporter, OSGiApplicationInfo osgiAppInfo) {
        super(deployer, env, reporter, osgiAppInfo);
    }

    @Override
    protected OSGiDeploymentContext getDeploymentContextImpl(ActionReport reporter, Logger logger, ReadableArchive source, UndeployCommandParameters undeployParams, ServerEnvironmentImpl env, Bundle bundle) throws Exception {
        return new OSGiEJBDeploymentContext(reporter, logger, source, undeployParams, env, bundle);
    }
}
