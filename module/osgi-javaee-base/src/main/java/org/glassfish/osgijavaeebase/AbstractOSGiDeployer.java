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

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.ActionReport;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.server.ServerEnvironmentImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public abstract class AbstractOSGiDeployer implements OSGiDeployer {

    /**
     * Various request processing states
     */
    public static enum State {
        DEPLOYING,
        DEPLOYED,
        FAILED,
        UNDEPLOYING,
        UNDEPLOYED
    }

    private static final Logger logger =
            Logger.getLogger(AbstractOSGiDeployer.class.getPackage().getName());

    private BundleContext bundleContext;
    private ServiceRegistration serviceReg;
    private int rank;

    private Deployment deployer = Globals.get(Deployment.class);
    private ArchiveFactory archiveFactory = Globals.get(ArchiveFactory.class);
    private ServerEnvironmentImpl env = Globals.get(ServerEnvironmentImpl.class);

    protected AbstractOSGiDeployer(BundleContext bundleContext, int rank) {
        this.bundleContext = bundleContext;
        this.rank = rank;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * Registers this as an OSGi service
     */
    public void register() {
        Properties properties = new Properties();
        properties.put(org.osgi.framework.Constants.SERVICE_RANKING, rank);
        serviceReg = bundleContext.registerService(OSGiDeployer.class.getName(), this, properties);
    }

    /**
     * Unregisters itself from OSGi service registry
     * Before it unregisters itself, it first undeploys all applications that were deployed using itself.
     */
    public void unregister() {
        /*
         * Why do we undeployAll while unregistering ourselves, but not deployAll during registering ourselves?
         * That's because, if we first unregister and rely on serviceRemoved() method to notify the OSGiContainer
         * to undeploy apps, OSGiContainer can't undeploy, because we are no longer available.  
         */
        undeployAll();
        serviceReg.unregister();
    }

    public OSGiApplicationInfo deploy(Bundle b) throws DeploymentException {
        raiseEvent(State.DEPLOYING, b, null);
        ActionReport report = getReport();
        OSGiDeploymentRequest request = createOSGiDeploymentRequest(deployer, archiveFactory, env, report, b);
        OSGiApplicationInfo osgiAppInfo = request.execute();
        if (osgiAppInfo == null) {
            final Throwable throwable = report.getFailureCause();
            raiseEvent(State.FAILED, b, throwable);
            throw new DeploymentException("Deployment of " + b + " failed because of following reason: " + report.getMessage(),
                    throwable);
        }
        raiseEvent(State.DEPLOYED, b, null);
        return osgiAppInfo;
    }

    public void undeploy(OSGiApplicationInfo osgiAppInfo) throws DeploymentException {
        final Bundle b = osgiAppInfo.getBundle();
        raiseEvent(State.UNDEPLOYING, b, null);
        ActionReport report = getReport();
        OSGiUndeploymentRequest request = createOSGiUndeploymentRequest(deployer, env, report, osgiAppInfo);
        request.execute();
        raiseEvent(State.UNDEPLOYED, b, null); // raise event even if something went wrong
        if (report.getActionExitCode() == ActionReport.ExitCode.FAILURE) {
            throw new DeploymentException("Undeployment of " + b + " failed because of following reason: " + report.getMessage(),
                    report.getFailureCause());
        }
    }

    protected ActionReport getReport() {
        // First of all, we can't get a reference to GlassFish service when server is stopping, because
        // GlassFish is first unregistered from registry when shutdown is called. Even if we we cache a reference to
        // GlassFish during startup, we can't use GlassFish.getService, because GlassFish would be in stopping state
        // and that would lead to IllegalStateException. So, use the ugly Globals API.
        return Globals.get(ActionReport.class);
    }

    /**
     * Undeploys all bundles which have been deployed using this deployer
     */
    public void undeployAll() {
        ServiceTracker st = new ServiceTracker(bundleContext, OSGiContainer.class.getName(), null);
        st.open();
        try {
            OSGiContainer c = (OSGiContainer) st.getService();
            if (c == null) return;
            ServiceReference deployerRef = serviceReg.getReference();
            for(OSGiApplicationInfo app : c.getDeployedApps()) {
                if (app.getDeployer() == deployerRef) {
                    try {
                        c.undeploy(app.getBundle());
                    } catch (Exception e) {
                        logger.logp(Level.WARNING, "WebExtender", "undeployAll", "Failed to undeploy bundle " + app.getBundle(), e);
                    }
                }
            }
        } finally {
            st.close();
        }
    }

    protected abstract OSGiDeploymentRequest createOSGiDeploymentRequest(Deployment deployer,
                                                      ArchiveFactory archiveFactory,
                                                      ServerEnvironmentImpl env,
                                                      ActionReport reporter,
                                                      Bundle b);

    protected abstract OSGiUndeploymentRequest createOSGiUndeploymentRequest(Deployment deployer,
                                                          ServerEnvironmentImpl env,
                                                          ActionReport reporter,
                                                          OSGiApplicationInfo osgiAppInfo);

    /**
     * Integration with Event Admin Service happens here.
     */
    protected void raiseEvent(State state, Bundle appBundle, Throwable throwable) {
    }

}
