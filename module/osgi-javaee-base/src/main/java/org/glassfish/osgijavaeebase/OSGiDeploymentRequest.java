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

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.server.ServerEnvironmentImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a stateful service. This is responsible for deployment of artifacts in JavaEE runtime.
 */
public abstract class OSGiDeploymentRequest {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(OSGiUndeploymentRequest.class.getPackage().getName());

    /**
     * GlassFish command reported.
     */
    private final ActionReport reporter;

    /**
     * Bundle to be deployed.
     */
    private final Bundle bundle;

    /**
     * Flag to indicate if this is directory deployment (opposed to archive).
     */
    private boolean dirDeployment;

    /**
     * GlassFish deployment object.
     */
    private final Deployment deployer;

    /**
     * GlassFish archive factory.
     */
    private final ArchiveFactory archiveFactory;

    /**
     * GlassFish server environment.
     */
    private final ServerEnvironmentImpl env;

    /**
     * GlassFish archive.
     */
    private ReadableArchive archive;

    /**
     * Custom deployment context.
     */
    private OSGiDeploymentContext dc;

    /**
     * Deployed application information.
     */
    private OSGiApplicationInfo result;

    /**
     * Create a new instance.
     * 
     * @param gfDeployer the GlassFish deployer
     * @param gfArchiveFactory the GlassFish archive factory
     * @param serverEnv the GlassFish server environment
     * @param actionReporter the GlassFish command reporter
     * @param appBundle the bundle to deploy
     */
    public OSGiDeploymentRequest(final Deployment gfDeployer, final ArchiveFactory gfArchiveFactory, final ServerEnvironmentImpl serverEnv,
            final ActionReport actionReporter, final Bundle appBundle) {

        this.deployer = gfDeployer;
        this.archiveFactory = gfArchiveFactory;
        this.env = serverEnv;
        this.reporter = actionReporter;
        this.bundle = appBundle;
    }

    /**
     * Invoked before the deploy action.
     * 
     * @throws DeploymentException if an error occurs
     */
    protected void preDeploy() throws DeploymentException {
    }

    /**
     * Invoked after the deploy action.
     */
    protected void postDeploy() {
    }

    /**
     * Deploys a web application bundle in GlassFish Web container. It properly rolls back if something goes wrong.
     *
     * @return OSGIApplicationInfo
     */
    public OSGiApplicationInfo execute() {
        try {
            preDeploy();
        } catch (DeploymentException e) {
            reporter.failure(LOGGER, "Failed while deploying bundle " + bundle, e);
            // return without calling postDeploy()
            return result;
        }
        // This is where the fun is...
        try {
            prepare();
            // Now actual deployment begins
            result = deploy();
        } catch (Exception e) {
            reporter.failure(LOGGER, "Failed while deploying bundle " + bundle, e);
        } finally {
            // call even if something failed so that the actions in
            // predeploy() can be rolled back.
            postDeploy();
        }
        return result;
    }

    /**
     * The steps are described below: 1. Create an Archive from the bundle - If the bundle has been installed with
     * reference: scheme, get hold hold of the underlying file and read from it, else use the bundle directly to create the
     * archive. 2. Prepare a context for deployment. This includes setting up various deployment options, setting up of an
     * ArchiveHandler, expansion of the archive, etc. The archive needs to be expanded before we create deployment context,
     * because in order to create WABClassLoader, we need to know expansion directory location, so that we can configure the
     * repositories correctly. More over, we need to create the deployment options before expanding the archive, because we
     * set application name = archive.getName(). If we explode first and then create OpsParams, then we will end up using
     * the same name as used by "asadmin deploy --type=osgi" and eventually hit by issue #10536. 3. Finally deploy and store
     * the result in our inmemory map.
     *
     * @throws Exception if an error occurs
     */
    private void prepare() throws Exception {

        archive = makeArchive();

        // Set up a deployment context
        OpsParams opsParams = getDeployParams();

        // expand if necessary, else set directory deployment to true
        expandIfNeeded();

        dc = getDeploymentContextImpl(reporter, LOGGER, archive, opsParams, env, bundle);
    }

    /**
     * Factory method. Subclasses override this to create specialized Archive instance.
     *
     * @return the created GlassFish archive instance for the bundle
     */
    protected ReadableArchive makeArchive() {
        return new OSGiBundleArchive(bundle);
    }

    /**
     * Get the deployment context for the given bundle.
     * 
     * @param actionReported the GlassFish command reporter
     * @param logger the logger to use
     * @param appArchive the GlassFish application archive
     * @param opsParams the GlassFish command parameters
     * @param serverEnv the GlassFish server environment
     * @param appBundle the application bundle
     * @return the deployment context
     * @throws Exception if an error occurs
     */
    protected abstract OSGiDeploymentContext getDeploymentContextImpl(ActionReport actionReported, Logger logger, ReadableArchive appArchive,
            OpsParams opsParams, ServerEnvironmentImpl serverEnv, Bundle appBundle) throws Exception;

    /**
     * Do the deployment work.
     * 
     * @return OSGiApplicationInfo
     */
    private OSGiApplicationInfo deploy() {
        // Need to declare outside to do proper cleanup of target dir
        // when deployment fails. We can't rely on exceptions as
        // deployer.deploy() eats most of the exceptions.
        ApplicationInfo appInfo = null;
        try {
            appInfo = deployer.deploy(dc);
            if (appInfo != null) {
                // Pass in the final classloader so that it can be used to set
                // appropriate context
                // while using underlying EE components in pure OSGi context
                // like registering EJB as services.
                // This won't be needed if we figure out a way of navigating
                // to the final classloader from
                // an EE component like EJB.
                return new OSGiApplicationInfo(appInfo, dirDeployment, bundle, dc.getFinalClassLoader());
            } else {
                LOGGER.logp(Level.FINE, "OSGiDeploymentRequest", "deploy", "failed to deploy {0} for following reason: {1} ",
                        new Object[] { bundle, reporter.getMessage() });
                throw new RuntimeException("Failed to deploy bundle [ " + bundle + " ], root cause: " + reporter.getMessage(), reporter.getFailureCause());
            }
        } finally {
            if (!dirDeployment && appInfo == null) {
                try {
                    File dir = dc.getSourceDir();
                    assert (dir.isDirectory());
                    if (FileUtils.whack(dir)) {
                        LOGGER.logp(Level.INFO, "OSGiDeploymentRequest", "deploy", "Deleted {0}", new Object[] { dir });
                    } else {
                        LOGGER.logp(Level.WARNING, "OSGiDeploymentRequest", "deploy", "Unable to delete {0} ", new Object[] { dir });
                    }
                } catch (Exception e2) {
                    LOGGER.logp(Level.WARNING, "OSGiDeploymentRequest", "deploy", "Exception while cleaning up target directory.", e2);
                    // don't throw this anymore
                }
            }
        }
    }

    /**
     * Expand the application archive on disk if needed.
     * 
     * @throws IOException if an error occurs
     */
    private void expandIfNeeded() throws IOException {

        // Try to obtain a handle to the underlying archive.
        // First see if it is backed by a file or a directory, else treat
        // it as a generic bundle.
        File file = makeFile(archive);

        // expand if necessary, else set directory deployment to true
        dirDeployment = file != null && file.isDirectory();
        if (dirDeployment) {
            LOGGER.logp(Level.FINE, "OSGiDeploymentRequest", "expandIfNeeded", "Archive is already expanded at = {0}", new Object[] { file });
            archive = archiveFactory.openArchive(file);
            return;
        }

        // ok we need to explode the archive somwhere and
        // remember to delete it on shutdown
        File tmpFile = getExplodedDir();
        tmpFile.deleteOnExit();

        // We don't reuse old directory at this point of time as we are not
        // completely sure if it is safe to reuse.
        // It is possible that we had a semicomplete directory from previous
        // execution.
        // e.g., if glassfish can be killed while osgi-container is exploding
        // the archive.
        // or glassfish can be killed while it was in the middle of deleting
        // the previously exploded directory.
        // So, better to not reuse existing directory until we ensure that
        // explosion or removal are atomic operations.
        if (tmpFile.exists()) {
            LOGGER.logp(Level.INFO, "OSGiDeploymentRequest", "expandIfNeeded", "Going to delete existing exploded content found at {0}",
                    new Object[] { tmpFile });
            if (!FileUtils.whack(tmpFile)) {
                throw new IOException("Unable to delete directory wth name " + tmpFile);
            }
        }
        if (!tmpFile.mkdirs()) {
            throw new IOException("Not able to expand " + archive.getName() + " in " + tmpFile);
        }
        WritableArchive targetArchive = archiveFactory.createArchive(tmpFile);
        new OSGiArchiveHandler().expand(archive, targetArchive, dc);
        LOGGER.logp(Level.INFO, "OSGiDeploymentRequest", "expandIfNeeded", "Expanded at {0}", new Object[] { targetArchive.getURI() });
        archive = archiveFactory.openArchive(tmpFile);
    }

    /**
     * We don't keep the file in tmpdir, because in some deployment environment, the tmpdir is periodically cleaned up by
     * external programs to reclaim memory. So, we keep them under application dir in bundle private storage area. More
     * over, we need to make sure that the directory is named uniquely to avoid accidental reuse. So, we include Bundle-Id
     * and Bundle-LastModifiedTimestamp in the dir name.
     *
     * @return the directory where the OSGi application will be exploded during deployment
     */
    private File getExplodedDir() {
        BundleContext ctx = getBundleContext(this.getClass());
        File bundleBaseStorage = ctx.getDataFile("");
        // We keep everything under application directory.
        // We include Bundle-Id and Bundle-Timestamp to ensure that we don't
        // accidentally use stale contents.
        // Assume the following where stale contents can be there in a file if
        // we don't use timestamp in name:
        // a wab deployed. Now system is stopped forcefully. During next restart
        // osgi-webcontainer
        // is not started, but wab is updated. Next time, osgi-web-container
        // comes up, it should not end up using
        // previously exploded directory.
        final String name = "applications" + File.separator + "bundle" + bundle.getBundleId() + "-" + bundle.getLastModified();
        return new File(bundleBaseStorage, name);
    }

    /**
     * Make a {@link File} instance for the given application archive.
     * 
     * @param a The archive
     * @return a File object that corresponds to this archive. return null if it can't determine the underlying file object.
     */
    public static File makeFile(final ReadableArchive a) {
        try {
            return new File(a.getURI());
        } catch (Exception e) {
            // Ignore, if we can't convert
        }
        return null;
    }

    /**
     * Get the GlassFish deploy command parameters.
     * 
     * @return DeployCommandParameters
     * @throws Exception if an error occurs
     */
    protected DeployCommandParameters getDeployParams() throws Exception {
        assert (archive != null);
        DeployCommandParameters parameters = new DeployCommandParameters();
        parameters.name = archive.getName();
        parameters.enabled = Boolean.TRUE;
        parameters.origin = DeployCommandParameters.Origin.deploy;
        parameters.force = false;
        parameters.target = getInstanceName();
        return parameters;
    }

    /**
     * Get the bundle for this deployment request.
     * 
     * @return Bundle
     */
    public Bundle getBundle() {
        return bundle;
    }

    /**
     * Get the archive for this deployment request.
     * 
     * @return ReadableArchive
     */
    public ReadableArchive getArchive() {
        return archive;
    }

    /**
     * Get the application info for this deployment request.
     * 
     * @return OSGiApplicationInfo
     */
    public OSGiApplicationInfo getResult() {
        return result;
    }

    /**
     * Get the GlassFish server instance name.
     * 
     * @return server instance name
     */
    private String getInstanceName() {
        ServerEnvironment se = Globals.get(ServerEnvironment.class);
        String target = se.getInstanceName();
        return target;
    }

    /**
     * Obtaining BundleContext which belongs to osgi-javaee-base.
     * 
     * @param clazz some class belongs to osgi-javaee-base
     * @return BundleContext which belongs to osgi-javaee-base
     */
    private BundleContext getBundleContext(final Class<?> clazz) {
        return BundleReference.class.cast(clazz.getClassLoader()).getBundle().getBundleContext();

    }
}
