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

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentContextImpl;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.osgi.framework.Bundle;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public abstract class OSGiDeploymentContext extends DeploymentContextImpl {

    protected ClassLoader shareableTempClassLoader;
    protected ClassLoader finalClassLoader;
    protected Bundle bundle;

    public OSGiDeploymentContext(ActionReport actionReport,
            Logger logger,
            ReadableArchive source,
            OpsParams params,
            ServerEnvironment env,
            Bundle bundle) throws Exception {
        super(actionReport, logger, source, params, env);
        this.bundle = bundle;
        setupClassLoader();

        // We always this handler instead of going through discovery process
        // which has issues.
        setArchiveHandler(new OSGiArchiveHandler());
    }

    protected abstract void setupClassLoader() throws Exception;

    @Override
    public void createDeploymentClassLoader(ClassLoaderHierarchy clh,
            ArchiveHandler handler)
            throws URISyntaxException, MalformedURLException {
        // do nothing as we override getClassLoader methods.
    }

    @Override
    public void createApplicationClassLoader(ClassLoaderHierarchy clh,
            ArchiveHandler handler)
            throws URISyntaxException, MalformedURLException {
        // do nothing as we override getClassLoader methods.
    }

    @Override
    public ClassLoader getClassLoader() {
        if (getPhase() != Phase.PREPARE) {
            // we return the final class loader
            return finalClassLoader;
        }
        return shareableTempClassLoader;
    }

    @Override
    public ClassLoader getFinalClassLoader() {
        return finalClassLoader;
    }

    @Override
    public synchronized ClassLoader getClassLoader(boolean sharable) {
        throw new RuntimeException("Assertion Failure: "
                + "This method should not be called");
    }
}
