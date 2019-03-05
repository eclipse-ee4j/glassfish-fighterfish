/*
 * Copyright (c) 2009, 2018 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.server.ServerEnvironmentImpl;
import org.osgi.framework.Bundle;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a stateful service. It is responsible for undeployment of the
 * artifact from JavaEE runtime.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public abstract class OSGiUndeploymentRequest {

    private static final Logger LOGGER = Logger.getLogger(
            OSGiUndeploymentRequest.class.getPackage().getName());

    private final Deployment deployer;
    private final ServerEnvironmentImpl env;
    private final ActionReport reporter;

    /**
     * Application being undeployed.
     */
    private final OSGiApplicationInfo osgiAppInfo;

    public OSGiUndeploymentRequest(Deployment deployer,
            ServerEnvironmentImpl env,
            ActionReport reporter,
            OSGiApplicationInfo osgiAppInfo) {

        this.deployer = deployer;
        this.env = env;
        this.reporter = reporter;
        this.osgiAppInfo = osgiAppInfo;
    }

    protected void preUndeploy() {
    }

    protected void postUndeploy() {
    }

    public void execute() {
        preUndeploy();
        // TODO(Sahoo): There may be side effect of creating a deployment
        // context as that leads to creation of class loaders again.
        OSGiDeploymentContext dc;
        try {
            dc = getDeploymentContextImpl(
                    reporter,
                    LOGGER,
                    osgiAppInfo.getAppInfo().getSource(),
                    getUndeployParams(osgiAppInfo),
                    env,
                    osgiAppInfo.getBundle());
        } catch (Exception e) {
            // TODO(Sahoo): Proper Exception Handling
            throw new RuntimeException(e);
        }

        final ApplicationInfo appInfo = osgiAppInfo.getAppInfo();
        appInfo.stop(dc, LOGGER);
        appInfo.unload(dc);
        deployer.undeploy(appInfo.getName(), dc);

        // GLASSFISH-19727: Close all class loaders. This must be done before
        // we clean up exploded directory
        // because WebappClassLoader is known to hold on to hold onto
        // references to WEB-INF/lib contents.
        // Ideally we should not have to do it. Deployer.undeploy is supposed
        // to close class loaders referenced
        // in DeploymentContext. But, there are two issues:
        // 1. In undeployment path, we use a different instance of class loader
        // as we don't have an easy way to
        // locate the class loader that was created during deployment.
        // 2. DeploymentContext.preDestroy does not call getClassLoader methods.
        // It uses fields directly.
        // We actually override the getClassLoader method and want that to be
        // used.
        closeClassLoaders(Arrays.asList(osgiAppInfo.getClassLoader(),
                dc.shareableTempClassLoader, dc.finalClassLoader));

        if (!osgiAppInfo.isDirectoryDeployment()) {
            // We can always assume dc.getSourceDir will return a valid file
            // because we would have expanded the app during deployment.
            cleanup(dc.getSourceDir());
        }
        postUndeploy();
    }

    private void closeClassLoaders(List<? extends Object> os) {
        for (Object o : os) {
            if (preDestroy(o)) {
                LOGGER.logp(Level.INFO, "OSGiUndeploymentRequest",
                        "closeClassLoaders",
                        "ClassLoader [ {0} ] has been closed.",
                        new Object[]{o});
            }
        }
    }

    /**
     * Calls preDestroy method. Since PreDestroy has changed incompatibly
     * between HK2 1.x and 2.x, We can't rely on class name. So, we use this
     * ugly work around to be compatible with both GF 3.x (which uses older
     * package name for PreDestroy and 4.0 (which uses new package name for
     * PreDestroy).
     *
     * @param o Object whose preDestroy method needs to be called if such a
     * method exists
     * @return true if such a method was successfully called, false otherwise
     */
    private boolean preDestroy(Object o) {
        Method m;
        try {
            m = o.getClass().getMethod("preDestroy");
            m.invoke(o);
            return true;
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    protected abstract OSGiDeploymentContext getDeploymentContextImpl(
            ActionReport reporter, Logger logger, ReadableArchive source,
            UndeployCommandParameters undeployParams,
            ServerEnvironmentImpl env, Bundle bundle) throws Exception;

    private void cleanup(File dir) {
        assert (dir.isDirectory() && dir.exists());
        FileUtils.whack(dir);
        LOGGER.logp(Level.INFO, "OSGiUndeploymentRequest", "cleanup",
                "Deleted {0}", new Object[]{dir});
    }

    protected UndeployCommandParameters getUndeployParams(
            OSGiApplicationInfo osgiAppInfo) {
        UndeployCommandParameters parameters
                = new UndeployCommandParameters();
        parameters.name = osgiAppInfo.getAppInfo().getName();
        parameters.origin = DeployCommandParameters.Origin.undeploy;
        return parameters;
    }

    protected OSGiApplicationInfo getOsgiAppInfo() {
        return osgiAppInfo;
    }
}
