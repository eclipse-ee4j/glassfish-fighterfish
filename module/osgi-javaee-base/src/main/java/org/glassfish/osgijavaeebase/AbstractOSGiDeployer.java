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
package org.glassfish.osgijavaeebase;

import java.util.Dictionary;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.ActionReport;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.server.ServerEnvironmentImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.sun.enterprise.deploy.shared.ArchiveFactory;

/**
 * Abstract implementation of {@link OSGiDeployer}.
 */
public abstract class AbstractOSGiDeployer implements OSGiDeployer {

    /**
     * Various request processing states.
     */
    public enum State {
        /**
         * Deployment in progress.
         */
        DEPLOYING,
        /**
         * Deployment.
         */
        DEPLOYED,
        /**
         * Deployment failed.
         */
        FAILED,
        /**
         * Undeployment in progress.
         */
        UNDEPLOYING,
        /**
         * Undeployed.
         */
        UNDEPLOYED
    }

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(AbstractOSGiDeployer.class.getPackage().getName());

    /**
     * The bundle context.
     */
    private final BundleContext bundleContext;

    /**
     * The service registration for {@link OSGiDeployer}.
     */
    private ServiceRegistration serviceReg;

    /**
     * The service rank to register the {@link OSGiDeployer} service.
     */
    private final int rank;

    /**
     * The GlassFish deployer.
     */
    private final Deployment deployer = Globals.get(Deployment.class);

    /**
     * The GlassFish archive factory.
     */
    private final ArchiveFactory archiveFactory = Globals.get(ArchiveFactory.class);

    /**
     * The GlassFish server environment.
     */
    private final ServerEnvironmentImpl env = Globals.get(ServerEnvironmentImpl.class);

    /**
     * Create a new instance.
     *
     * @param ctx the bundle context
     * @param svcRank the deployer service rank
     */
    protected AbstractOSGiDeployer(final BundleContext ctx, final int svcRank) {

        this.bundleContext = ctx;
        this.rank = svcRank;
    }

    /**
     * Get the bundle context.
     *
     * @return BundleContext
     */
    public final BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * Registers this as an OSGi service.
     */
    @SuppressWarnings("unchecked")
    public final void register() {
        Dictionary properties = new Properties();
        properties.put(org.osgi.framework.Constants.SERVICE_RANKING, rank);
        serviceReg = bundleContext.registerService(OSGiDeployer.class.getName(), this, properties);
    }

    /**
     * Unregisters itself from OSGi service registry Before it unregisters itself, it first undeploys all applications that
     * were deployed using itself.
     */
    public final void unregister() {
        // Why do we undeployAll while unregistering ourselves, but not
        // deployAll during registering ourselves?
        // That's because, if we first unregister and rely on serviceRemoved()
        // method to notify the OSGiContainer
        // to undeploy apps, OSGiContainer can't undeploy, because we are no
        // longer available.
        undeployAll();
        serviceReg.unregister();
    }

    @Override
    public final OSGiApplicationInfo deploy(final Bundle bnd) throws DeploymentException {

        raiseEvent(State.DEPLOYING, bnd, null);
        ActionReport report = getReport();
        OSGiDeploymentRequest request = createOSGiDeploymentRequest(deployer, archiveFactory, env, report, bnd);
        OSGiApplicationInfo osgiAppInfo = request.execute();
        if (osgiAppInfo == null) {
            final Throwable throwable = report.getFailureCause();
            raiseEvent(State.FAILED, bnd, throwable);
            throw new DeploymentException("Deployment of " + bnd + " failed because of following reason: " + report.getMessage(), throwable);
        }
        raiseEvent(State.DEPLOYED, bnd, null);
        return osgiAppInfo;
    }

    @Override
    public final void undeploy(final OSGiApplicationInfo osgiAppInfo) throws DeploymentException {

        final Bundle b = osgiAppInfo.getBundle();
        raiseEvent(State.UNDEPLOYING, b, null);
        ActionReport report = getReport();
        OSGiUndeploymentRequest request = createOSGiUndeploymentRequest(deployer, env, report, osgiAppInfo);
        request.execute();
        // raise event even if something went wrong
        raiseEvent(State.UNDEPLOYED, b, null);
        if (report.getActionExitCode() == ActionReport.ExitCode.FAILURE) {
            throw new DeploymentException("Undeployment of " + b + " failed because of following reason: " + report.getMessage(), report.getFailureCause());
        }
    }

    /**
     * Get the GlassFish command reporter.
     *
     * @return ActionReport
     */
    protected final ActionReport getReport() {
        // First of all, we can't get a reference to GlassFish service when
        // server is stopping, because
        // GlassFish is first unregistered from registry when shutdown is
        // called. Even if we we cache a reference to
        // GlassFish during startup, we can't use GlassFish.getService,
        // because GlassFish would be in stopping state
        // and that would lead to IllegalStateException. So, use the ugly
        // Globals API.
        return Globals.get(ActionReport.class);
    }

    /**
     * Undeploys all bundles which have been deployed using this deployer.
     */
    @SuppressWarnings("unchecked")
    public final void undeployAll() {
        ServiceTracker st = new ServiceTracker(bundleContext, OSGiContainer.class.getName(), null);
        st.open();
        try {
            OSGiContainer c = (OSGiContainer) st.getService();
            if (c == null) {
                return;
            }
            ServiceReference deployerRef = serviceReg.getReference();
            for (OSGiApplicationInfo app : c.getDeployedApps()) {
                if (app.getDeployer() == deployerRef) {
                    try {
                        c.undeploy(app.getBundle());
                    } catch (Exception e) {
                        LOGGER.logp(Level.WARNING, "WebExtender", "undeployAll", "Failed to undeploy bundle " + app.getBundle(), e);
                    }
                }
            }
        } finally {
            st.close();
        }
    }

    /**
     * Create an OSGi deployment request.
     *
     * @param gfDeployer the GlassFish deployer
     * @param gfArchiveFactory the GlassFish archive factory
     * @param gfServerEnv the GlassFish server environment
     * @param gfCmdReporter the GlassFish command reporter
     * @param bnd the application bundle
     * @return OSGiDeploymentRequest
     */
    protected abstract OSGiDeploymentRequest createOSGiDeploymentRequest(Deployment gfDeployer, ArchiveFactory gfArchiveFactory,
            ServerEnvironmentImpl gfServerEnv, ActionReport gfCmdReporter, Bundle bnd);

    /**
     * Create an OSGi undeployment request.
     *
     * @param gfDeployer the GlassFish deployer
     * @param gfServerEnv the GlassFish server environment
     * @param gfCmdReporter the GlassFish command reporter
     * @param osgiAppInfo the deployed application info
     * @return OSGiUndeploymentRequest
     */
    protected abstract OSGiUndeploymentRequest createOSGiUndeploymentRequest(Deployment gfDeployer, ServerEnvironmentImpl gfServerEnv,
            ActionReport gfCmdReporter, OSGiApplicationInfo osgiAppInfo);

    /**
     * Integration with Event Admin Service happens here.
     *
     * @param state bundle state
     * @param bnd the bundle
     * @param throwable the exception at the source of the event
     */
    protected void raiseEvent(final State state, final Bundle bnd, final Throwable throwable) {
    }
}
